package com.smartcrawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.core.CrawlerEngine;
import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.model.IndexPayload;
import com.smartcrawler.platform.ServiceInstaller;
import com.smartcrawler.platform.impl.LinuxServiceInstaller;
import com.smartcrawler.platform.impl.MacOSServiceInstaller;
import com.smartcrawler.platform.impl.WindowsServiceInstaller;
import com.smartcrawler.core.SchedulerService;
import com.smartcrawler.ui.MainApp;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Point d'entrée de l'application Smart Crawler Agent.
 * Parse les arguments de la ligne de commande et initialise les services ou l'UI.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private static SchedulerService schedulerService;
    private static CrawlerEngine crawlerEngine;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.info("Aucun argument fourni. Démarrage en mode graphique par défaut (--gui).", null);
            args = new String[] { "--gui" };
        }

        // Initialize services
        ConfigManager configManager = ConfigManager.getInstance();
        schedulerService = new SchedulerService();
        crawlerEngine = new CrawlerEngine();

        // Initialize shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application", null);
            if (schedulerService != null) {
                schedulerService.shutdown();
            }
            if (crawlerEngine != null) {
                crawlerEngine.shutdown();
            }
        }));

        String command = args[0];
        switch (command) {
            case "--service":
                startService();
                break;
            case "--crawl":
                if (args.length < 2) {
                    logger.error("Missing URL for --crawl", null);
                    printUsage();
                    System.exit(1);
                }
                crawlUrl(args[1]);
                break;
            case "--install-service":
                installService();
                break;
            case "--config-test":
                testConfig();
                break;
            case "--gui":
                startGui(args);
                break;
            default:
                logger.error("Unknown command: " + command, null);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar smart-crawler-agent.jar [command]\n");
        System.out.println("Commands:");
        System.out.println("  --service           Start the agent in service mode");
        System.out.println("  --crawl <url>       Crawl a specific URL");
        System.out.println("  --install-service   Install the agent as a system service");
        System.out.println("  --config-test       Test the configuration");
        System.out.println("  --gui               Start the graphical user interface\n");
        System.out.println("Si aucun argument n'est fourni, le mode --gui est utilisé par défaut.");
    }

    private static void startService() {
        logger.info("Starting in service mode", null);
        try {
            schedulerService.startScheduler();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Service interrupted", e);
        }
    }

    private static void crawlUrl(String url) {
        try {
            CrawlConfig config = new CrawlConfig();
            IndexPayload payload = new IndexPayload();
            logger.info("Initiating crawl for URL: " + url, null);
            crawlerEngine.startCrawl(config, url, payload);
            if (!payload.getResults().isEmpty()) {
                saveCrawlResults(payload);
                logger.info("Crawl results saved for URL: " + url, null);
            }
            logger.info("Crawl completed successfully for: " + url, null);
        } catch (Exception e) {
            logger.error("Error during crawl: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void saveCrawlResults(IndexPayload payload) {
        String reportFile = "reports/crawl-report.json";
        try {
            List<IndexPayload> existingPayloads = new ArrayList<>();
            Path path = Paths.get(reportFile);
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path));
                existingPayloads = objectMapper.readValue(content, objectMapper.getTypeFactory().constructCollectionType(List.class, IndexPayload.class));
            }
            existingPayloads.add(payload);
            Files.createDirectories(path.getParent());
            Files.write((Path) Files.newOutputStream(path), objectMapper.writeValueAsBytes(existingPayloads));
            logger.info("Crawl results saved to: " + reportFile, null);
        } catch (IOException e) {
            logger.error("Error saving crawl results", e);
        }
    }

    private static void installService() {
        logger.info("Installing service", null);
        try {
            ServiceInstaller installer;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                installer = new LinuxServiceInstaller();
            } else if (os.contains("windows")) {
                installer = new WindowsServiceInstaller();
            } else if (os.contains("mac")) {
                installer = new MacOSServiceInstaller();
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }
            installer.install();
            logger.info("Service installed successfully", null);
        } catch (IOException e) {
            logger.error("Error installing service", e);
            System.exit(1);
        }
    }

    private static void testConfig() {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            String defaultProfile = configManager.getProperty("crawler.default.profile", "standard");
            logger.info("Configuration valid. Default profile: " + defaultProfile, null);
        } catch (Exception e) {
            logger.error("Error during configuration test: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void startGui(String[] args) {
        logger.info("Starting GUI mode", null);
        MainApp.initServices(schedulerService, crawlerEngine);
        MainApp.launch(MainApp.class, args);
    }
}