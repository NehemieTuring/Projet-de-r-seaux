package com.searchengine.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base exception for search engine-related errors.
 */
public class SearchEngineException extends RuntimeException {

    private static final Logger logger = LoggerFactory.getLogger(SearchEngineException.class);

    public SearchEngineException(String message) {
        super(message);
        logger.error("SearchEngineException: {}", message);
    }

    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
        logger.error("SearchEngineException: {}", message, cause);
    }
}