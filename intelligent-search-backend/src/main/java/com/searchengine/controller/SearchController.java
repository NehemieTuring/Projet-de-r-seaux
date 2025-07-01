package com.searchengine.controller;

import com.searchengine.model.dto.request.SearchRequestDto;
import com.searchengine.model.dto.response.SearchResponseDto;
import com.searchengine.service.ScoringService;
import com.searchengine.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller pour gérer les requêtes de recherche.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "API pour rechercher des documents indexés")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;
    private final ScoringService scoringService;

    public SearchController(SearchService searchService, ScoringService scoringService) {
        this.searchService = searchService;
        this.scoringService = scoringService;
    }

    /**
     * Exécute une requête de recherche et retourne les documents correspondants.
     *
     * @param searchRequestDto La requête de recherche
     * @param httpRequest La requête HTTP pour extraire l'IP et l'User-Agent
     * @return ResponseEntity avec les résultats de la recherche
     */
    @Operation(summary = "Rechercher des documents", description = "Exécute une requête de recherche et retourne les documents correspondants")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats de recherche retournés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête de recherche invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    @PostMapping
    public ResponseEntity<SearchResponseDto> search(@Valid @RequestBody SearchRequestDto searchRequestDto, HttpServletRequest httpRequest) {
        logger.info("Requête de recherche reçue avec la requête : {}", searchRequestDto.getQuery());
        try {
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            SearchResponseDto response = searchService.search(searchRequestDto, ipAddress, userAgent);
//            response.setResults(scoringService.applyScoring(response.getResults(), searchRequest.getQuery(), response.getContext()));
            logger.debug("Recherche terminée avec {} résultats pour la requête : {}", response.getResults().size(), searchRequestDto.getQuery());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Échec du traitement de la requête de recherche pour la requête : {}", searchRequestDto.getQuery(), e);
            throw e; // Géré par GlobalExceptionHandler
        }
    }
}