package com.searchengine.service;

import com.searchengine.model.dto.request.AnalyticsRequest;
import com.searchengine.model.dto.response.AnalyticsResponse;
import com.searchengine.model.entity.SearchAnalytics;
import com.searchengine.repository.elasticsearch.AnalyticsRepository;
import com.searchengine.repository.jpa.AnalyticsJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Service for collecting and processing analytics data.
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsJpaRepository analyticsJpaRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository, AnalyticsJpaRepository analyticsJpaRepository) {
        this.analyticsRepository = analyticsRepository;
        this.analyticsJpaRepository = analyticsJpaRepository;
    }

    /**
     * Retrieves analytics data based on the provided request.
     *
     * @param request The analytics request
     * @return AnalyticsResponse with processed metrics
     */
    public AnalyticsResponse getAnalytics(AnalyticsRequest request) {
        logger.info("Processing analytics request for time range: {} to {}",
                request.getStartTime(), request.getEndTime());
        try {
            // Convert Instant to LocalDateTime for JPA repository
            LocalDateTime startTime = LocalDateTime.ofInstant(request.getStartTime(), ZoneOffset.UTC);
            LocalDateTime endTime = LocalDateTime.ofInstant(request.getEndTime(), ZoneOffset.UTC);

            // Fetch from Elasticsearch for real-time analytics
            List<SearchAnalytics> esAnalytics = analyticsRepository.findByTimestampBetween(
                    request.getStartTime(), request.getEndTime());

            // Fetch from PostgreSQL for historical analytics
            List<SearchAnalytics> jpaAnalytics = analyticsJpaRepository.findByTimestampBetween(
                    startTime, endTime);

            // Combine and process results
            AnalyticsResponse response = new AnalyticsResponse();
            response.setMetrics(combineAnalytics(esAnalytics, jpaAnalytics));
            response.setTotalSearches(esAnalytics.size() + jpaAnalytics.size());
            logger.debug("Retrieved {} analytics records", response.getTotalSearches());
            return response;
        } catch (Exception e) {
            logger.error("Failed to process analytics request: {}", request, e);
            throw new RuntimeException("Analytics processing failed", e);
        }
    }

    /**
     * Combines analytics data from Elasticsearch and PostgreSQL.
     *
     * @param esAnalytics Elasticsearch analytics
     * @param jpaAnalytics PostgreSQL analytics
     * @return Combined list of analytics
     */
    private List<SearchAnalytics> combineAnalytics(List<SearchAnalytics> esAnalytics, List<SearchAnalytics> jpaAnalytics) {
        // Simple merge (can be enhanced with deduplication logic)
        List<SearchAnalytics> combined = new java.util.ArrayList<>(esAnalytics);
        combined.addAll(jpaAnalytics);
        return combined;
    }
}