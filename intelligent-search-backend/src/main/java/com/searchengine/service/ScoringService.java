package com.searchengine.service;

import com.searchengine.component.scoring.ScoreCalculator;
import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour calculer et appliquer des scores aux résultats de recherche.
 */
@Service
public class ScoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScoringService.class);
    private final ScoreCalculator scoreCalculator;

    @Value("${app.scoring.weight.popularity:0.4}")
    private double popularityWeight;

    @Value("${app.scoring.weight.freshness:0.3}")
    private double freshnessWeight;

    @Value("${app.scoring.weight.relevance:0.3}")
    private double relevanceWeight;

    @Value("${app.scoring.weight.location:0.0}")
    private double locationWeight;

    public ScoringService(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    /**
     * Applique un score à une liste de documents basée sur les poids configurés.
     *
     * @param documents Liste des documents à scorer
     * @param query Requête de recherche pour le scoring de pertinence
     * @param context Contexte de recherche pour le scoring de localisation
     * @return Liste des documents avec les scores mis à jour
     */
    public List<DocumentResponse> applyScoring(List<DocumentResponse> documents, String query, SearchContext context) {
        logger.info("Application du scoring à {} documents pour la requête : {}", documents.size(), query);
        try {
            return documents.stream()
                    .peek(document -> {
                        double score = scoreCalculator.calculateScore(
                                document, context, popularityWeight, freshnessWeight,  locationWeight);
                        document.setScore(score);
                        logger.debug("Score calculé {} pour le document : {}", score, document.getUrl());
                    })
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Échec de l'application du scoring pour la requête : {}", query, e);
            throw new RuntimeException("Échec du scoring pour la requête : " + query, e);
        }
    }
}