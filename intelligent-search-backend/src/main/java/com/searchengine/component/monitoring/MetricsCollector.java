package com.searchengine.component.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Component for collecting and exposing JMX and Actuator metrics.
 */
@Component
@ManagedResource(objectName = "com.searchengine:name=MetricsCollector,type=Metrics")
public class MetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    private final AtomicLong indexedDocuments = new AtomicLong(0);
    private final AtomicLong indexingErrors = new AtomicLong(0);
    private final Timer indexingTimer;

    @Autowired
    public MetricsCollector(MeterRegistry meterRegistry) {
        this.indexingTimer = Timer.builder("searchengine.indexing.time")
                .description("Time taken to index documents")
                .register(meterRegistry);
        logger.info("MetricsCollector initialized");
    }

    /**
     * Increments the count of indexed documents.
     */
    public void incrementIndexedDocuments() {
        indexedDocuments.incrementAndGet();
        logger.debug("Incremented indexed documents count: {}", indexedDocuments.get());
    }

    /**
     * Increments the count of indexing errors.
     */
    public void incrementIndexingErrors() {
        indexingErrors.incrementAndGet();
        logger.warn("Incremented indexing errors count: {}", indexingErrors.get());
    }

    /**
     * Records the time taken for an indexing operation.
     *
     * @param durationMillis Duration in milliseconds
     */
    public void recordIndexingTime(long durationMillis) {
        indexingTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.debug("Recorded indexing time: {}ms", durationMillis);
    }

    /**
     * Gets the total number of indexed documents.
     *
     * @return Total indexed documents
     */
    @ManagedAttribute(description = "Total number of indexed documents")
    public long getIndexedDocuments() {
        return indexedDocuments.get();
    }

    /**
     * Gets the total number of indexing errors.
     *
     * @return Total indexing errors
     */
    @ManagedAttribute(description = "Total number of indexing errors")
    public long getIndexingErrors() {
        return indexingErrors.get();
    }
}