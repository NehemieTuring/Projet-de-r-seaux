package com.smartcrawler.core;

import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.model.IndexPayload;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import lombok.Setter;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de planification pour exécuter les crawls périodiquement.
 * Gère une file d'attente de tâches et expose des métriques JMX.
 */
public class SchedulerService implements SchedulerServiceMBean {

    private final Logger logger;
    private final ConfigManager configManager;
    private final CrawlerEngine crawlerEngine;
    private ScheduledExecutorService scheduler;
    private final BlockingQueue<String> urlQueue;
    private final AtomicInteger activeCrawls;
    private final int maxQueueSize;
    private final int maxConcurrentCrawls;
    private final List<String> scheduledUrls; // Liste des URLs planifiées
    @Setter
    private CrawlResultListener crawlResultListener;

    public SchedulerService() {
        this.logger = LoggerFactory.getLogger(SchedulerService.class.getName());
        this.configManager = ConfigManager.getInstance();
        this.crawlerEngine = new CrawlerEngine();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.maxQueueSize = configManager.getIntProperty("scheduler.max.queue.size", 1000);
        this.maxConcurrentCrawls = configManager.getIntProperty("crawler.max.concurrent.crawls", 3);
        this.urlQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.activeCrawls = new AtomicInteger(0);
        this.scheduledUrls = new ArrayList<>();
        registerMBean();

        if (configManager.getBooleanProperty("scheduler.enabled", true)) {
            startScheduler();
        }
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.smartcrawler.core:type=SchedulerService");
            mbs.registerMBean(this, name);
            logger.info("JMX MBean registered for SchedulerService", null);
        } catch (Exception e) {
            logger.error("Error registering JMX MBean: " + e.getMessage(), e);
        }
    }

    @Override
    public int getQueueSize() {
        return urlQueue.size();
    }

    @Override
    public int getActiveCrawls() {
        return activeCrawls.get();
    }

    /**
     * Interface pour notifier les résultats de crawl.
     */
    public interface CrawlResultListener {
        void onCrawlResult(IndexPayload payload);
    }

    /**
     * Démarre le planificateur pour exécuter les crawls à intervalles réguliers.
     */
    public void startScheduler() {
        if (!scheduler.isShutdown() && !scheduler.isTerminated()) {
            logger.warn("Scheduler already running", null);
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        restorePendingUrls();
        long intervalMinutes = configManager.getIntProperty("scheduler.interval.minutes", 30);
        logger.info("Démarrage du planificateur, intervalle: " + intervalMinutes + " minutes", null);
        scheduler.scheduleAtFixedRate(this::processQueue, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Ajoute une URL à la file d'attente pour le crawl.
     *
     * @param url URL à crawler
     * @return true si l'URL est ajoutée, false si la file est pleine ou l'URL est déjà planifiée
     */
    public boolean scheduleCrawl(String url) {
        if (urlQueue.size() >= maxQueueSize) {
            logger.warn("File d'attente pleine, URL rejetée: " + url, null);
            return false;
        }
        if (scheduledUrls.contains(url)) {
            logger.info("URL already scheduled: " + url, null);
            return false;
        }
        try {
            urlQueue.put(url);
            scheduledUrls.add(url);
            logger.debug("URL ajoutée à la file: " + url, null);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interruption lors de l'ajout de l'URL: " + url, e);
            return false;
        }
    }

    /**
     * Déclenche un crawl immédiat pour une URL spécifique.
     *
     * @param url URL à crawler immédiatement
     */
    public void triggerImmediateCrawl(String url) {
        if (activeCrawls.get() >= maxConcurrentCrawls) {
            logger.warn("Max concurrent crawls reached, queuing URL: " + url, null);
            scheduleCrawl(url);
            return;
        }
        try {
            activeCrawls.incrementAndGet();
            CrawlConfig config = new CrawlConfig();
            CompletableFuture.supplyAsync(() -> {
                        IndexPayload payload = new IndexPayload();
                        crawlerEngine.startCrawl(config, url, payload);
                        return payload;
                    }, Executors.newFixedThreadPool(maxConcurrentCrawls))
                    .thenAccept(payload -> {
                        if (crawlResultListener != null) {
                            crawlResultListener.onCrawlResult(payload);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error during immediate crawl of URL: " + url, throwable);
                        return null;
                    })
                    .whenComplete((result, throwable) -> activeCrawls.decrementAndGet());
        } catch (Exception e) {
            activeCrawls.decrementAndGet();
            logger.error("Error initiating immediate crawl for URL: " + url, e);
        }
    }

    /**
     * Retire une URL de la file d'attente.
     *
     * @param url URL à retirer
     */
    public void removeUrl(String url) {
        urlQueue.remove(url);
        scheduledUrls.remove(url);
        logger.info("URL removed from scheduler: " + url, null);
    }

    /**
     * Met à jour une URL dans la file d'attente.
     *
     * @param oldUrl Ancienne URL
     * @param newUrl Nouvelle URL
     */
    public void updateUrl(String oldUrl, String newUrl) {
        if (urlQueue.remove(oldUrl)) {
            scheduledUrls.remove(oldUrl);
            scheduleCrawl(newUrl);
            logger.info("URL updated in scheduler: " + oldUrl + " to " + newUrl, null);
        }
    }

    private void processQueue() {
        if (activeCrawls.get() >= maxConcurrentCrawls) {
            logger.debug("Nombre maximum de crawls concurrents atteint: " + activeCrawls.get(), null);
            return;
        }

        String url = urlQueue.peek(); // Ne pas retirer l'URL, la garder pour re-crawling
        if (url == null) {
            logger.debug("File d'attente vide", null);
            return;
        }

        try {
            activeCrawls.incrementAndGet();
            CrawlConfig config = new CrawlConfig();
            CompletableFuture.supplyAsync(() -> {
                        IndexPayload payload = new IndexPayload();
                        crawlerEngine.startCrawl(config, url, payload);
                        return payload;
                    }, Executors.newFixedThreadPool(maxConcurrentCrawls))
                    .thenAccept(payload -> {
                        if (crawlResultListener != null) {
                            crawlResultListener.onCrawlResult(payload);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Erreur lors du crawl de l'URL: " + url, throwable);
                        return null;
                    })
                    .whenComplete((result, throwable) -> {
                        activeCrawls.decrementAndGet();
                        // Re-planifier l'URL pour le prochain cycle
                        if (!urlQueue.isEmpty()) {
                            String nextUrl = urlQueue.poll();
                            urlQueue.offer(nextUrl);
                            logger.debug("URL re-scheduled: " + nextUrl, null);
                        }
                    });
        } catch (Exception e) {
            activeCrawls.decrementAndGet();
            logger.error("Erreur lors du traitement de l'URL: " + url, e);
        }
    }

    /**
     * Arrête le planificateur de manière gracieuse.
     */
    public void shutdown() {
        logger.info("Arrêt du planificateur", null);
        savePendingUrls();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("Arrêt forcé du planificateur", null);
            }
            while (activeCrawls.get() > 0) {
                Thread.sleep(1000);
                logger.debug("Waiting for active crawls to finish: " + activeCrawls.get(), null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interruption lors de l'arrêt du planificateur", e);
        }
    }

    private void savePendingUrls() {
        try {
            List<String> pendingUrls = new ArrayList<>();
            urlQueue.drainTo(pendingUrls);
            Files.writeString(Paths.get("pending-urls.txt"), String.join("\n", pendingUrls));
            logger.debug("Saved " + pendingUrls.size() + " pending URLs", null);
        } catch (IOException e) {
            logger.error("Error saving pending URLs", e);
        }
    }

    public void restorePendingUrls() {
        try {
            Path path = Paths.get("pending-urls.txt");
            if (Files.exists(path)) {
                List<String> urls = Files.readAllLines(path);
                urls.forEach(this::scheduleCrawl);
                Files.deleteIfExists(path);
                logger.debug("Restored " + urls.size() + " pending URLs", null);
            }
        } catch (IOException e) {
            logger.error("Error restoring pending URLs", e);
        }
    }
}