package com.searchengine.model.dto.response;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for health check response.
 */
@Data
public class HealthResponse {

    private String status;
    private Map<String, ComponentStatus> components = new HashMap<>();

    /**
     * Adds a component status to the health response.
     *
     * @param name Component name
     * @param status Component status
     */
    public void addComponent(String name, String status) {
        components.put(name, new ComponentStatus(status, null));
    }

    /**
     * Adds a component status with error message to the health response.
     *
     * @param name Component name
     * @param status Component status
     * @param errorMessage Error message
     */
    public void addComponent(String name, String status, String errorMessage) {
        components.put(name, new ComponentStatus(status, errorMessage));
    }

    /**
     * Inner class for component status.
     */
    @Data
    private static class ComponentStatus {
        private String status;
        private String errorMessage;

        public ComponentStatus(String status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }
}