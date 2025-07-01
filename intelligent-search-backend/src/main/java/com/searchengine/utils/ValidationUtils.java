package com.searchengine.utils;

import com.searchengine.model.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Utility class for validating URLs and data payloads.
 */
public class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    /**
     * Validates if a URL is well-formed and uses allowed protocols (http, https).
     *
     * @param url URL to validate
     * @return true if valid, false otherwise
     * @throws IllegalArgumentException if URL is malformed
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            logger.warn("Invalid URL: null or empty");
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                logger.warn("Unsupported protocol in URL: {}", url);
                return false;
            }
            return true;
        } catch (URISyntaxException e) {
            logger.error("Malformed URL: {}", url, e);
            return false;
        }
    }

    /**
     * Validates if a content type is supported.
     *
     * @param contentType Content type to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidContentType(DocumentType contentType) {
        if (contentType == null) {
            logger.warn("Invalid content type: null");
            return false;
        }
        return true;
    }
}