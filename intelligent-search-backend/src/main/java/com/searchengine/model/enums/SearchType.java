package com.searchengine.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the types of search queries.
 */
public enum SearchType {

    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    ALL("all");

    private final String value;

    SearchType(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the search type.
     *
     * @return String value of the search type
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts a string to a SearchType enum.
     *
     * @param value String value to convert
     * @return Corresponding SearchType or ALL if not found
     */
    public static SearchType fromValue(String value) {
        if (value == null) {
            return ALL;
        }
        for (SearchType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return ALL;
    }
}