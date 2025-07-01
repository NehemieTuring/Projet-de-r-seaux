package com.searchengine.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the types of data sources.
 */
public enum SourceType {

    WEBSITE("website"),
    API("api"),
    DATABASE("database"),
    FILE("file");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the source type.
     *
     * @return String value of the source type
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts a string to a SourceType enum.
     *
     * @param value String value to convert
     * @return Corresponding SourceType or WEBSITE if not found
     */
    public static SourceType fromValue(String value) {
        if (value == null) {
            return WEBSITE;
        }
        for (SourceType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return WEBSITE;
    }
}