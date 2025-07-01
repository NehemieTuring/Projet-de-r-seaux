package com.smartcrawler.utils;

import com.smartcrawler.core.ConfigManager;

/**
 * Fabrique pour créer des instances de Logger.
 * Gère la configuration JSON basée sur ConfigManager.
 */
public class LoggerFactory {

    private static volatile boolean initialized = false;
    private static boolean jsonFormatEnabled = true;

    /**
     * Initialise la fabrique avec la configuration de ConfigManager.
     */
    private static synchronized void initialize() {
        if (!initialized) {
            try {
                ConfigManager config = ConfigManager.getInstance();
                jsonFormatEnabled = config.getBooleanProperty("logging.json.format", true);
                initialized = true;
            } catch (Exception e) {
                // Fallback to default if ConfigManager fails
                jsonFormatEnabled = true;
                initialized = true;
            }
        }
    }

    /**
     * Crée une instance de Logger pour le nom donné.
     *
     * @param name Nom du logger
     * @return Instance de Logger
     */
    public static Logger getLogger(String name) {
        initialize();
        return new JsonLogger(name, jsonFormatEnabled);
    }
}