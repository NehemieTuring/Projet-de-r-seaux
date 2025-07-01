package com.searchengine.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the status of an indexing operation.
 */
public enum IndexStatus {

    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    IndexStatus(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the index status.
     *
     * @return String value of the index status
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts a string to an IndexStatus enum.
     *
     * @param value String value to convert
     * @return Corresponding IndexStatus or PENDING if not found
     */
    public static IndexStatus fromValue(String value) {
        if (value == null) {
            return PENDING;
        }
        for (IndexStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING;
    }
}