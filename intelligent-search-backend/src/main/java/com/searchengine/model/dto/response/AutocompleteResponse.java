package com.searchengine.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO pour la réponse d'autocomplétion contenant des suggestions et une complétion intelligente.
 */
@Data
@Builder(toBuilder = true)
public class AutocompleteResponse {

    /**
     * Liste des suggestions basées sur les titres des documents.
     */
    private final List<String> suggestions;

    /**
     * Suggestion unique pour l'autocomplétion intelligente (mot suivant ou extension).
     */
    private final String completion;

    /**
     * Indique si la complétion est une extension du préfixe saisi (true) ou un mot suivant (false).
     */
    private final boolean isExtension;
}