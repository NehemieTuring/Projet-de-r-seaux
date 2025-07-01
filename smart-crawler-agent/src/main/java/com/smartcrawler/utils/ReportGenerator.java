package com.smartcrawler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.IndexPayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Générateur de rapports périodiques pour les crawls.
 */
public class ReportGenerator {

    private final Logger logger;
    private final ConfigManager configManager;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final List<IndexPayload> payloads;

    public ReportGenerator() {
        this.logger = LoggerFactory.getLogger(ReportGenerator.class.getName());
        this.configManager = ConfigManager.getInstance();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.payloads = new ArrayList<>();
        startReportScheduler();
    }

    /**
     * Ajoute un payload pour inclusion dans le prochain rapport.
     *
     * @param payload Payload à inclure
     */
    public void addPayload(IndexPayload payload) {
        synchronized (payloads) {
            payloads.add(payload);
            logger.debug("Payload added for reporting, batchId: " + payload.getBatchId(), null);
        }
    }

    private void startReportScheduler() {
        long intervalMinutes = configManager.getIntProperty("reporting.interval.minutes", 60);
        scheduler.scheduleAtFixedRate(this::generateReport, 0, intervalMinutes, TimeUnit.MINUTES);
        logger.info("Report generator started, interval: " + intervalMinutes + " minutes", null);
    }

    private void generateReport() {
        List<IndexPayload> currentPayloads;
        synchronized (payloads) {
            if (payloads.isEmpty()) {
                logger.debug("No payloads to report", null);
                return;
            }
            currentPayloads = new ArrayList<>(payloads);
            payloads.clear();
        }

        try {
            String reportFile = configManager.getProperty("logging.report.file", "reports/crawl-report.json");
            String reportContent = objectMapper.writeValueAsString(currentPayloads);
            FileUtils.writeToFile(reportFile, reportContent);
            logger.info("Report generated successfully: " + reportFile, null);
        } catch (IOException e) {
            logger.error("Error generating report", e);
        }
    }

    /**
     * Arrête le générateur de rapports.
     */
    public void shutdown() {
        logger.info("Shutting down report generator", null);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("Forced shutdown of report generator", null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during shutdown", e);
        }
    }
}
