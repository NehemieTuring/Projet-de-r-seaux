package com.searchengine.component.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component for managing system alerts.
 */
@Component
public class AlertManager {

    private static final Logger logger = LoggerFactory.getLogger(AlertManager.class);

    @Value("${app.alerts.enabled:false}")
    private boolean alertsEnabled;

    /**
     * Sends an alert for a critical system event.
     *
     * @param message Alert message
     * @param throwable Optional throwable for error details
     */
    public void sendAlert(String message, Throwable throwable) {
        if (!alertsEnabled) {
            logger.debug("Alerts are disabled, ignoring alert: {}", message);
            return;
        }
        try {
            // Placeholder: Implement actual alert mechanism (e.g., email, Slack)
            logger.error("ALERT: {} - {}", message, throwable != null ? throwable.getMessage() : "No exception");
        } catch (Exception e) {
            logger.error("Failed to send alert: {}", message, e);
        }
    }
}