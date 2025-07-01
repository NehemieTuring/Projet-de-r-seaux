package com.searchengine.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for autocomplete request payload.
 */
@Data
public class AutocompleteRequest {

    @NotBlank(message = "Query prefix cannot be blank")
    private String prefix;

    private String sessionId;

    private String language;

    private Integer maxSuggestions = 10;
}