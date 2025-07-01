package com.smartcrawler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Logger structuré en JSON pour un parsing facile.
 * Implémente l'interface Logger pour découpler de ConfigManager.
 */
public class JsonLogger implements Logger {

    private final org.slf4j.Logger logger;
    private final ObjectMapper objectMapper;
    private final boolean jsonFormatEnabled;

    public JsonLogger(String name, boolean jsonFormatEnabled) {
        this.logger = LoggerFactory.getLogger(name);
        this.objectMapper = new ObjectMapper();
        this.jsonFormatEnabled = jsonFormatEnabled;
    }

    @Override
    public void info(String message, Throwable throwable) {
        log("INFO", message, throwable);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        log("WARN", message, throwable);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        log("DEBUG", message, throwable);
    }

    private void log(String level, String message, Throwable throwable) {
        if (jsonFormatEnabled) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("level", level);
            logEntry.put("message", message);
            logEntry.put("timestamp", System.currentTimeMillis());
            if (throwable != null) {
                logEntry.put("error", throwable.getMessage());
                logEntry.put("stackTrace", getStackTraceAsString(throwable));
            }
            try {
                String jsonLog = objectMapper.writeValueAsString(logEntry);
                switch (level) {
                    case "INFO":
                        logger.info(jsonLog);
                        break;
                    case "WARN":
                        logger.warn(jsonLog);
                        break;
                    case "ERROR":
                        logger.error(jsonLog, throwable);
                        break;
                    case "DEBUG":
                        logger.debug(jsonLog);
                        break;
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la sérialisation du log JSON: " + message, e);
            }
        } else {
            switch (level) {
                case "INFO":
                    logger.info(message, throwable);
                    break;
                case "WARN":
                    logger.warn(message, throwable);
                    break;
                case "ERROR":
                    logger.error(message, throwable);
                    break;
                case "DEBUG":
                    logger.debug(message, throwable);
                    break;
            }
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}