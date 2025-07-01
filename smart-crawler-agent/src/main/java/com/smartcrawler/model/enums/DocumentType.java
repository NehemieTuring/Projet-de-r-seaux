package com.smartcrawler.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Types de documents supportés par le système (côté agent et backend).
 */
public enum DocumentType {
    PDF, IMAGE, VIDEO, AUDIO, API, DOCUMENT, WEB_PAGE, UNKNOWN;

    @JsonCreator
    public static DocumentType fromString(String key) {
        return Arrays.stream(DocumentType.values())
                .filter(e -> e.name().equalsIgnoreCase(key))
                .findFirst()
                .orElse(UNKNOWN);
    }

    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}
