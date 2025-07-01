package com.searchengine.service;

import com.searchengine.component.monitoring.AlertManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing notifications and alerts.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final AlertManager alertManager;

    @Autowired
    public NotificationService(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    /**
     * Sends a notification for a critical event.
     *
     * @param message The notification message
     * @param throwable Optional throwable for error details
     */
    public void sendNotification(String message, Throwable throwable) {
        logger.info("Sending notification: {}", message);
        try {
            alertManager.sendAlert(message, throwable);
            logger.debug("Notification sent successfully: {}", message);
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", message, e);
        }
    }

    /**
     * Sends a user-specific notification.
     *
     * @param sessionId The session ID of the user
     * @param message The notification message
     */
    public void sendUserNotification(String sessionId, String message) {
        logger.info("Sending user notification for session: {}", sessionId);
        try {
            // Placeholder: Implement actual user notification logic (e.g., WebSocket, email)
            logger.debug("User notification sent for session: {} - {}", sessionId, message);
        } catch (Exception e) {
            logger.error("Failed to send user notification for session: {}", sessionId, e);
        }
    }
}