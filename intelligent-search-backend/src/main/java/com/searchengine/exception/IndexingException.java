package com.searchengine.exception;

/**
 * Custom exception for indexing-related errors.
 */
public class IndexingException extends RuntimeException {

    private final String errorCode;

    public IndexingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public IndexingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with the exception.
     *
     * @return Error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    // Common error codes
    public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
    public static final String ELASTICSEARCH_DOWN = "ELASTICSEARCH_DOWN";
}