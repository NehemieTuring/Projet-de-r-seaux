package com.searchengine.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for analytics request payload.
 */
@Data
public class AnalyticsRequest {

    @NotNull(message = "Start time cannot be null")
    private Instant startTime;

    @NotNull(message = "End time cannot be null")
    private Instant endTime;

    private String actionType; // Optional filter for SEARCH or INDEX
}