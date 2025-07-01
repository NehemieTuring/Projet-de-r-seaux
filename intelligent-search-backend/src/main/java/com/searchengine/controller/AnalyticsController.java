package com.searchengine.controller;

import com.searchengine.model.dto.request.AnalyticsRequest;
import com.searchengine.model.dto.response.AnalyticsResponse;
import com.searchengine.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling analytics requests.
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "API for retrieving search and indexing analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Retrieves analytics data based on the provided request.
     *
     * @param analyticsRequest The analytics request payload
     * @return ResponseEntity with analytics data
     */
    @Operation(summary = "Retrieve analytics", description = "Fetches analytics data for searches and indexing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics data returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid analytics request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics(@Valid @RequestBody AnalyticsRequest analyticsRequest) {
        logger.info("Received analytics request for time range: {} to {}",
                analyticsRequest.getStartTime(), analyticsRequest.getEndTime());
        try {
            AnalyticsResponse response = analyticsService.getAnalytics(analyticsRequest);
            logger.debug("Analytics retrieved successfully: {} metrics", response.getMetrics().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve analytics for request: {}", analyticsRequest, e);
            throw e; // Handled by GlobalExceptionHandler
        }
    }
}