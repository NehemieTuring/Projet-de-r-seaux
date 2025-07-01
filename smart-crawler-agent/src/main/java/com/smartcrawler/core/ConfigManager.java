package com.smartcrawler.core;

import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestionnaire de configuration centralisé (Singleton).
 * Charge et fournit un accès aux propriétés des fichiers application.properties et crawler-profiles.properties.
 */
public class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();
    private final Properties properties;
    private Logger logger;

    private ConfigManager() {
        properties = new Properties();
        // Logger will be initialized after properties are loaded
        loadProperties();
        this.logger = LoggerFactory.getLogger(ConfigManager.class.getName());
    }

    /**
     * Retourne l'instance unique du ConfigManager.
     *
     * @return Instance singleton de ConfigManager
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * Charge les fichiers de propriétés depuis les ressources.
     */
    private void loadProperties() {
        try {
            // Charger application.properties
            loadPropertyFile("application.properties");
            // Charger crawler-profiles.properties
            loadPropertyFile("crawler-profiles.properties");
            // Initialize logger only after properties are loaded
            if (logger == null) {
                logger = LoggerFactory.getLogger(ConfigManager.class.getName());
            }
            logger.info("Propriétés chargées avec succès", null);
        } catch (IOException e) {
            // Use a temporary SLF4J logger if initialization fails
            if (logger == null) {
                org.slf4j.Logger tempLogger = org.slf4j.LoggerFactory.getLogger(ConfigManager.class.getName());
                tempLogger.error("Erreur lors du chargement des propriétés", e);
            } else {
                logger.error("Erreur lors du chargement des propriétés", e);
            }
            throw new RuntimeException("Échec du chargement des fichiers de configuration", e);
        }
    }

    private void loadPropertyFile(String fileName) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IOException("Fichier de propriétés introuvable : " + fileName);
            }
            properties.load(input);
        }
    }

    /**
     * Récupère une propriété avec une valeur par défaut si absente.
     *
     * @param key          Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur de la propriété ou valeur par défaut
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Récupère une propriété sous forme d'entier avec une valeur par défaut.
     *
     * @param key          Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur entière de la propriété
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Format invalide pour la propriété " + key + ", utilisation de la valeur par défaut : " + defaultValue, e);
            }
        }
        return defaultValue;
    }

    /**
     * Récupère une propriété sous forme de booléen avec une valeur par défaut.
     *
     * @param key          Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur booléenne de la propriété
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }
}