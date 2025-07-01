package com.searchengine.model.dto.request;

import com.searchengine.model.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * DTO for search request payload.
 */
@Data
public class SearchRequestDto {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    @PositiveOrZero(message = "Page must be zero or positive")
    private int page = 0;

    @Positive(message = "Size must be positive")
    private int size = 10;

    // Nouveau champ pour le type de document (optionnel)
    private DocumentType documentType;

    // Nouveau champ pour la langue (optionnel, format ISO 639-1, ex: "fr", "en")
    @Pattern(regexp = "^$|^[a-zA-Z]{2}$", message = "Language must be a valid ISO 639-1 code (e.g., 'fr', 'en')")
    private String language;
}