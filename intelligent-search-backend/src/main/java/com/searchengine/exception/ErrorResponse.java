package com.searchengine.exception;

import lombok.Data;

/**
 * DTO for error responses.
 */
@Data
public class ErrorResponse {

    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}