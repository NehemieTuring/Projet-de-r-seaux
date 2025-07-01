package com.searchengine.component.scoring;

import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Composant optimisé pour calculer les scores de fraîcheur avec algorithmes avancés.
 */
@Component
public class FreshnessScorer {

    private static final Logger logger = LoggerFactory.getLogger(FreshnessScorer.class);

    @Value("${app.scoring.freshness.max-age-days:30}")
    private int maxAgeDays;

    @Value("${app.scoring.freshness.decay-factor:0.1}")
    private double decayFactor;

    @Value("${app.scoring.freshness.boost-threshold-hours:24}")
    private int boostThresholdHours;

    /**
     * Calcule le score de fraîcheur avec fonction de décroissance exponentielle.
     */
    public double calculateFreshnessScore(DocumentResponse document) {
        logger.debug("Calcul du score de fraîcheur pour le document: {}", document.getUrl());
        try {
            Instant crawlInstant = document.getCrawlTimestamp();
            if (crawlInstant == null) {
                logger.warn("Aucun timestamp de crawl pour le document: {}", document.getUrl());
                return 0.0;
            }

            Instant now = Instant.now();

            if (crawlInstant.isAfter(now)) {
                logger.warn("Timestamp de crawl invalide (dans le futur) pour le document: {}", document.getUrl());
                return 0.0;
            }

            long ageInHours = ChronoUnit.HOURS.between(crawlInstant, now);
            double freshnessScore = calculateAdvancedFreshnessScore(ageInHours);

            logger.debug("Score de fraîcheur pour {} : {} (âge: {} heures)",
                    document.getUrl(), freshnessScore, ageInHours);
            return freshnessScore;

        } catch (Exception e) {
            logger.error("Échec du calcul du score de fraîcheur pour le document: {}", document.getUrl(), e);
            return 0.0;
        }
    }

    /**
     * Surcharge pour Document entity.
     */
    public double calculateFreshnessScore(SearchDocument searchDocument) {
        logger.debug("Calcul du score de fraîcheur pour le document: {}", searchDocument.getUrl());
        try {
            Instant crawlInstant = searchDocument.getCrawlTimestamp();
            if (crawlInstant == null) {
                logger.warn("Aucun timestamp de crawl pour le document: {}", searchDocument.getUrl());
                return 0.0;
            }

            Instant now = Instant.now();

            if (crawlInstant.isAfter(now)) {
                logger.warn("Timestamp de crawl invalide (dans le futur) pour le document: {}", searchDocument.getUrl());
                return 0.0;
            }

            long ageInHours = ChronoUnit.HOURS.between(crawlInstant, now);
            double freshnessScore = calculateAdvancedFreshnessScore(ageInHours);

            logger.debug("Score de fraîcheur pour {} : {} (âge: {} heures)",
                    searchDocument.getUrl(), freshnessScore, ageInHours);
            return freshnessScore;

        } catch (Exception e) {
            logger.error("Échec du calcul du score de fraîcheur pour le document: {}", searchDocument.getUrl(), e);
            return 0.0;
        }
    }

    /**
     * Calcule le score avec algorithme de décroissance exponentielle avancé.
     */
    private double calculateAdvancedFreshnessScore(long ageInHours) {
        if (ageInHours <= boostThresholdHours) {
            double recentBoost = 1.0 + (0.2 * (1.0 - (ageInHours / (double) boostThresholdHours)));
            return Math.min(1.0, recentBoost);
        }

        double ageInDays = ageInHours / 24.0;

        if (ageInDays > maxAgeDays) {
            return Math.max(0.01, Math.exp(-ageInDays / maxAgeDays * 3));
        }

        double normalizedAge = ageInDays / maxAgeDays;
        double exponentialDecay = Math.exp(-decayFactor * normalizedAge * 10);
        double linearDecay = Math.max(0.0, 1.0 - normalizedAge);

        double combinedScore = (0.3 * linearDecay) + (0.7 * exponentialDecay);
        return Math.max(0.01, Math.min(1.0, combinedScore));
    }

    /**
     * Calcule un score de fraîcheur relatif basé sur une fenêtre temporelle.
     */
    public double calculateRelativeFreshnessScore(DocumentResponse document, long referenceTimestamp) {
        try {
            Instant documentTimestamp = document.getCrawlTimestamp();
            if (documentTimestamp == null) {
                return 0.0;
            }

            long timeDiff = Math.abs(documentTimestamp.toEpochMilli() - referenceTimestamp);
            long maxDiff = maxAgeDays * 24L * 60L * 60L * 1000L;

            if (timeDiff > maxDiff) {
                return 0.0;
            }

            return 1.0 - (timeDiff / (double) maxDiff);

        } catch (Exception e) {
            logger.error("Échec du calcul du score de fraîcheur relatif", e);
            return 0.0;
        }
    }

    /**
     * Détermine si un document est considéré comme "frais".
     */
    public boolean isFresh(DocumentResponse document) {
        return calculateFreshnessScore(document) > 0.5;
    }

    /**
     * Détermine si un document est très récent (boost applicable).
     */
    public boolean isVeryRecent(DocumentResponse document) {
        try {
            Instant crawlInstant = document.getCrawlTimestamp();
            if (crawlInstant == null) {
                return false;
            }

            long ageInHours = ChronoUnit.HOURS.between(crawlInstant, Instant.now());
            return ageInHours <= boostThresholdHours;

        } catch (Exception e) {
            logger.error("Erreur dans isVeryRecent", e);
            return false;
        }
    }
}
