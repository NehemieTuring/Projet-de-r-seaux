package com.searchengine.controller;

import com.searchengine.exception.SearchException;
import com.searchengine.model.dto.request.AutocompleteRequest;
import com.searchengine.model.dto.response.AutocompleteResponse;
import com.searchengine.service.AutocompleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autocomplete")
@Tag(name = "Autocomplete", description = "API for autocomplete suggestions")
public class AutocompleteController {

    private static final Logger logger = LoggerFactory.getLogger(AutocompleteController.class);
    private final AutocompleteService autocompleteService;

    @Autowired
    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    /**
     * Provides autocomplete suggestions for a query prefix.
     *
     * @param request The autocomplete request
     * @return ResponseEntity with list of suggestions
     */
    @Operation(summary = "Get autocomplete suggestions", description = "Fetches suggestions based on query prefix using hybrid strategy (Elasticsearch + smart logic)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<AutocompleteResponse> getSuggestions(@Valid @RequestBody AutocompleteRequest request) {
        logger.info("Processing autocomplete request for prefix: {}", request.getPrefix());
        try {
            AutocompleteResponse response = autocompleteService.getAutocompleteSuggestions(
                    request.getPrefix(), request.getLanguage(), request.getMaxSuggestions());

            logger.debug("Suggestions: {}, Completion: {}",
                    response.getSuggestions().size(), response.getCompletion());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve autocomplete suggestions for prefix: {}", request.getPrefix(), e);
            throw new SearchException("Failed to retrieve autocomplete suggestions", e);
        }
    }
}
