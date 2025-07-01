package com.smartcrawler.utils;

import com.smartcrawler.core.ConfigManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilitaires pour la validation des URLs et des données.
 */
public class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class.getName());
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?$");
    private static final Set<String> ALLOWED_PROTOCOLS = new HashSet<>();

    static {
        ConfigManager config = ConfigManager.getInstance();
        String protocols = config.getProperty("security.allowed.protocols", "http,https");
        ALLOWED_PROTOCOLS.addAll(Arrays.asList(protocols.split(",")));
    }

    /**
     * Valide si une URL est correcte et respecte les protocoles autorisés.
     *
     * @param url URL à valider
     * @return true si l'URL est valide, false sinon
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            if (!ALLOWED_PROTOCOLS.contains(protocol)) {
                logger.warn("Protocole non autorisé : " + protocol + " pour l'URL : " + url, null);
                return false;
            }
            return true;
        } catch (MalformedURLException e) {
            logger.debug("URL invalide (MalformedURLException) : " + url, null);
            return false;
        }
    }

}