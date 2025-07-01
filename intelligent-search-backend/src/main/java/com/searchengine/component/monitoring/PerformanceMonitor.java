package com.searchengine.component.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Component for monitoring system performance metrics.
 */
@Component
public class PerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    private final MeterRegistry meterRegistry;
    private final Timer searchTimer;
    private final Timer indexTimer;

    @Autowired
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.searchTimer = Timer.builder("searchengine.search.time")
                .description("Time taken for search operations")
                .register(meterRegistry);
        this.indexTimer = Timer.builder("searchengine.index.time")
                .description("Time taken for indexing operations")
                .register(meterRegistry);
        logger.info("PerformanceMonitor initialized");
    }

    /**
     * Records the time taken for a search operation.
     *
     * @param durationMillis Duration in milliseconds
     */
    public void recordSearchTime(long durationMillis) {
        searchTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.debug("Recorded search time: {}ms", durationMillis);
    }

    /**
     * Records the time taken for an indexing operation.
     *
     * @param durationMillis Duration in milliseconds
     */
    public void recordIndexTime(long durationMillis) {
        indexTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.debug("Recorded index time: {}ms", durationMillis);
    }
}