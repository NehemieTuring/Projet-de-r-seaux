package com.searchengine.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception for errors occurring during search operations.
 */
public class SearchException extends SearchEngineException {

    private static final Logger logger = LoggerFactory.getLogger(SearchException.class);

    public SearchException(String message) {
        super(message);
        logger.error("SearchException: {}", message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
        logger.error("SearchException: {}", message, cause);
    }
}