package com.searchengine.component.scoring;

import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchContext;
import com.searchengine.utils.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composant pour calculer les scores basés sur la localisation des documents.
 */
@Component
public class LocationScorer {

    private static final Logger logger = LoggerFactory.getLogger(LocationScorer.class);

    @Value("${app.scoring.location.max-distance-km:1000}")
    private double maxDistanceKm;

    /**
     * Calcule le score de localisation pour un document basé sur la géolocalisation de l'utilisateur.
     *
     * @param document Le document à scorer
     * @param searchContext Le contexte de recherche contenant la géolocalisation de l'utilisateur
     * @return Score de localisation normalisé entre 0 et 1
     */
    public double calculateLocationScore(DocumentResponse document, SearchContext searchContext) {
        logger.debug("Calcul du score de localisation pour le document : {}", document.getUrl());
        try {
            if (searchContext == null || searchContext.getGeoLocation() == null || document.getMetadata() == null) {
                logger.debug("Géolocalisation ou métadonnées manquantes pour le document : {}", document.getUrl());
                return 0.0;
            }

            // Extraire la localisation du document à partir des métadonnées (suppose que latitude/longitude sont stockées)
            String latStr = document.getMetadata().get("latitude");
            String lonStr = document.getMetadata().get("longitude");
            if (latStr == null || lonStr == null) {
                logger.debug("Aucune donnée de localisation dans les métadonnées du document : {}", document.getUrl());
                return 0.0;
            }

            SearchContext.GeoLocation docLocation = new SearchContext.GeoLocation();
            docLocation.setLatitude(Double.parseDouble(latStr));
            docLocation.setLongitude(Double.parseDouble(lonStr));

            double distance = GeoUtils.calculateDistance(searchContext.getGeoLocation(), docLocation);
            double locationScore = Math.max(0.0, 1.0 - (distance / maxDistanceKm));
            logger.debug("Score de localisation pour {} : {} (distance : {} km)", document.getUrl(), locationScore, distance);
            return locationScore;
        } catch (Exception e) {
            logger.error("Échec du calcul du score de localisation pour le document : {}", document.getUrl(), e);
            return 0.0;
        }
    }
}