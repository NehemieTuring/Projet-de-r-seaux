package com.searchengine.model.dto.response;

import com.searchengine.model.entity.SearchAnalytics;
import lombok.Data;

import java.util.List;

/**
 * DTO for analytics response containing metrics.
 */
@Data
public class AnalyticsResponse {

    private List<SearchAnalytics> metrics;
    private long totalSearches;
}