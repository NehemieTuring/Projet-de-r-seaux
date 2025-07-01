package com.smartcrawler.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.crawler.ApiDiscoverer;
import com.smartcrawler.crawler.DynamicContentDetector;
import com.smartcrawler.crawler.WebCrawler;
import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.model.CrawlResult;
import com.smartcrawler.model.IndexPayload;
import com.smartcrawler.model.enums.CrawlStatus;
import com.smartcrawler.service.BackendClientService;
import com.smartcrawler.service.RateLimiter;
import com.smartcrawler.service.SecurityManager;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.ValidationUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Moteur principal du crawler, responsable de l'orchestration des opérations de crawl.
 * Intègre les crawlers, détecteurs, communication backend, et métriques JMX.
 */
public class CrawlerEngine implements CrawlerEngineMBean {

    private final Logger logger;
    private final WebCrawler webCrawler;
    private final DynamicContentDetector dynamicContentDetector;
    private final ApiDiscoverer apiDiscoverer;
    private final BackendClientService backendClientService;
    private final RateLimiter rateLimiter;
    private final SecurityManager securityManager;
    private final ExecutorService executorService;
    private final Set<String> visitedUrls;
    private final ConfigManager configManager;
    private final AtomicLong crawlCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong activeTasks = new AtomicLong(0);
    private final ConcurrentHashMap<String, String> previousContents = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CrawlerEngineListener listener;

    public CrawlerEngine() {
        this.logger = LoggerFactory.getLogger(CrawlerEngine.class.getName());
        this.webCrawler = new WebCrawler();
        this.dynamicContentDetector = new DynamicContentDetector();
        this.apiDiscoverer = new ApiDiscoverer();
        this.backendClientService = new BackendClientService();
        this.rateLimiter = new RateLimiter();
        this.securityManager = new SecurityManager();
        this.configManager = ConfigManager.getInstance();
        int poolSize = configManager.getIntProperty("crawler.connection.pool.size", 20);
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        registerMBean();
    }

    /**
     * Interface pour notifier les résultats de crawl.
     */
    public interface CrawlerEngineListener {
        void onCrawlResult(IndexPayload payload);
    }

    public void setCrawlerEngineListener(CrawlerEngineListener listener) {
        this.listener = listener;
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.smartcrawler.core:type=CrawlerEngine");

            if (!mbs.isRegistered(name)) {
                mbs.registerMBean(this, name);
                logger.info("JMX MBean registered for CrawlerEngine", null);
            } else {
                logger.warn("JMX MBean already registered: " + name, null);
            }
        } catch (Exception e) {
            logger.error("Error registering JMX MBean: " + e.getMessage(), e);
        }
    }

    @Override
    public long getCrawlCount() {
        return crawlCount.get();
    }

    @Override
    public long getErrorCount() {
        return errorCount.get();
    }

    @Override
    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }

    public void startCrawl(CrawlConfig config, String url, IndexPayload payload) {
        if (!ValidationUtils.isValidUrl(url)) {
            logger.error("Invalid URL: " + url, null);
            errorCount.incrementAndGet();
            throw new IllegalArgumentException("URL cannot be null or invalid");
        }

        if (!securityManager.isUrlAllowed(url)) {
            logger.warn("URL blocked by robots.txt or security rules: " + url, null);
            return;
        }

        logger.info("Starting crawl for URL: " + url, null);
        activeTasks.incrementAndGet();
        try {
            crawlRecursive(config, url, 0, payload);
            crawlCount.incrementAndGet();

            if (!payload.getResults().isEmpty()) {
                // Filtrer les nouveaux contenus
                IndexPayload newContentPayload = filterNewContent(payload);
                if (!newContentPayload.getResults().isEmpty()) {
                    if (backendClientService.sendPayload(newContentPayload)) {
                        logger.info("New content payload sent successfully, batchId: " + newContentPayload.getBatchId(), null);
                    } else {
                        logger.error("Failed to send new content payload, batchId: " + newContentPayload.getBatchId(), null);
                        errorCount.incrementAndGet();
                    }
                }
                // Sauvegarder tous les résultats pour l'historique
                saveCrawlResults(payload);
                // Notifier le listener
                if (listener != null) {
                    listener.onCrawlResult(payload);
                }
            }

            logger.info("Crawl completed for URL: " + url, null);
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    private IndexPayload filterNewContent(IndexPayload payload) {
        IndexPayload newPayload = new IndexPayload();
        newPayload.setBatchId(payload.getBatchId());
        newPayload.setCrawlSession(payload.getCrawlSession());
        newPayload.setTimestamp(payload.getTimestamp());
        for (CrawlResult result : payload.getResults()) {
            if (result.getStatus() == CrawlStatus.SUCCESS && result.getContent() != null) {
                String previousContent = previousContents.getOrDefault(result.getUrl(), "");
                if (!previousContent.equals(result.getContent())) {
                    newPayload.addResult(result);
                    previousContents.put(result.getUrl(), result.getContent());
                    logger.info("New content detected for URL: " + result.getUrl(), null);
                }
            }
        }
        return newPayload;
    }

    private void saveCrawlResults(IndexPayload payload) {
        String reportFile = "reports/crawl-report.json";
        Path path = Paths.get(reportFile);

        try {
            Files.createDirectories(path.getParent());

            List<IndexPayload> existingPayloads = new ArrayList<>();

            if (Files.exists(path)) {
                String content = Files.readString(path).trim();

                if (!content.isEmpty() && content.startsWith("[") && content.endsWith("]")) {
                    existingPayloads = objectMapper.readValue(
                            content,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, IndexPayload.class)
                    );
                } else {
                    logger.warn("Crawl report content invalid or empty. Resetting with fresh list.", null);
                }
            } else {
                Files.writeString(path, "[]");
            }

            existingPayloads.add(payload);
            Files.write(path, objectMapper.writeValueAsBytes(existingPayloads));
            logger.info("Crawl results saved to: " + reportFile, null);
        } catch (IOException e) {
            logger.error("Error saving crawl results", e);
        }
    }

    private void crawlRecursive(CrawlConfig config, String url, int depth, IndexPayload payload) {
        if (depth > config.getMaxDepth() || visitedUrls.size() >= config.getMaxPagesPerDomain()) {
            logger.debug("Depth or page limit reached for URL: " + url, null);
            return;
        }

        if (visitedUrls.contains(url)) {
            logger.debug("URL already visited: " + url, null);
            return;
        }

        if (!rateLimiter.allowRequest(url)) {
            logger.warn("Rate limit exceeded for URL: " + url, null);
            return;
        }

        visitedUrls.add(url);

        try {
            CrawlResult result = webCrawler.crawl(config, url, depth);

            if (result.getStatus() == CrawlStatus.SUCCESS) {
                if (result.getContent() == null || result.getContent().isBlank()) {
                    logger.warn("Skipping URL with empty content: " + url, null);
                    errorCount.incrementAndGet();
                    return;
                }

                // Ajouter les résultats des documents (PDF, médias, etc.)
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent(configManager.getProperty("crawler.user.agent", "SmartCrawler/1.0"))
                            .timeout(configManager.getIntProperty("crawler.request.timeout", 10000))
                            .get();
                    List<CrawlResult> documentResults = webCrawler.extractDocumentResults(doc, config, url);
                    for (CrawlResult docResult : documentResults) {
                        payload.addResult(docResult);
                    }
                } catch (IOException e) {
                    logger.debug("Erreur lors de l'extraction des documents pour : " + url, e);
                }

                if (dynamicContentDetector.isDynamicContent(url, config)) {
                    result.setDynamic(true);
                    result.setStatus(CrawlStatus.DYNAMIC);
                }

                if (config.isDetectApis()) {
                    result.setApiEndpoints(apiDiscoverer.discoverApis(config, url, depth));
                }

                payload.addResult(result);

                if (result.getLinks() != null) {
                    for (String nextUrl : result.getLinks()) {
                        if (isAllowedLink(nextUrl, config, url, depth)) {
                            activeTasks.incrementAndGet();
                            executorService.submit(() -> {
                                try {
                                    crawlRecursive(config, nextUrl, depth + 1, payload);
                                } finally {
                                    activeTasks.decrementAndGet();
                                }
                            });
                        }
                    }
                }
            } else {
                payload.addResult(result);
                errorCount.incrementAndGet();
            }
        } catch (Exception e) {
            logger.error("Error crawling URL: " + url, e);
            CrawlResult errorResult = new CrawlResult();
            errorResult.setUrl(url);
            errorResult.setStatus(CrawlStatus.ERROR);
            errorResult.setErrorMessage(e.getMessage());
            payload.addResult(errorResult);
            errorCount.incrementAndGet();
        }
    }

    private boolean isAllowedLink(String nextUrl, CrawlConfig config, String currentUrl, int depth) {
        if (!ValidationUtils.isValidUrl(nextUrl) || !securityManager.isUrlAllowed(nextUrl)) {
            return false;
        }

        try {
            String currentDomain = new java.net.URL(currentUrl).getHost();
            String nextDomain = new java.net.URL(nextUrl).getHost();
            if (!currentDomain.equals(nextDomain) && !config.isFollowExternalLinks()) {
                return false;
            }
            if (!currentDomain.equals(nextDomain) && config.getMaxExternalDepth() < depth + 1) {
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error validating link: " + nextUrl, e);
            return false;
        }
    }

    public void shutdown() {
        logger.info("Shutting down crawler engine", null);
        executorService.shutdown();
        try {
            while (activeTasks.get() > 0) {
                logger.debug("Waiting for " + activeTasks.get() + " active crawl tasks to complete", null);
                Thread.sleep(1000);
            }
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                logger.warn("Forced shutdown of crawler engine, remaining tasks interrupted", null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during shutdown", e);
            executorService.shutdownNow();
        }
    }

    public boolean isRunning() {
        return !executorService.isShutdown();
    }
}