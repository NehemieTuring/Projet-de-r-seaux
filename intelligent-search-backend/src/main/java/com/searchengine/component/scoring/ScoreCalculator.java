package com.searchengine.component.scoring;

import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Composant optimisé pour calculer les scores des documents avec mise en cache et algorithmes performants.
 * Note : Pertinence calculée uniquement via Elasticsearch, donc absente ici.
 */
@Component
public class ScoreCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ScoreCalculator.class);

    private final ConcurrentHashMap<String, CachedScore> scoreCache = new ConcurrentHashMap<>();

    @Value("${app.scoring.cache.ttl:300000}") // 5 minutes
    private long cacheTtl;

    private final LocationScorer locationScorer;
    private final FreshnessScorer freshnessScorer;
    private final PopularityScorer popularityScorer;

    public ScoreCalculator(LocationScorer locationScorer,
                           FreshnessScorer freshnessScorer,
                           PopularityScorer popularityScorer) {
        this.locationScorer = locationScorer;
        this.freshnessScorer = freshnessScorer;
        this.popularityScorer = popularityScorer;
    }

    /**
     * Calcule le score global sans pertinence locale.
     */
    public double calculateScore(DocumentResponse document, SearchContext context,
                                 double popularityWeight, double freshnessWeight, double locationWeight) {

        String cacheKey = generateCacheKey(document, context, popularityWeight, freshnessWeight, locationWeight);

        CachedScore cached = scoreCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Score récupéré du cache pour: {}", document.getUrl());
            return cached.score;
        }

        logger.debug("Calcul du score pour le document : {}", document.getUrl());

        try {
            double totalWeight = popularityWeight + freshnessWeight + locationWeight;
            if (totalWeight == 0) totalWeight = 1.0;

            double popularityScore = popularityScorer.calculatePopularityScore(document);
            double freshnessScore = freshnessScorer.calculateFreshnessScore(document);
            double locationScore = locationScorer.calculateLocationScore(document, context);

            double finalScore = (popularityScore * popularityWeight +
                    freshnessScore * freshnessWeight +
                    locationScore * locationWeight) / totalWeight;

            scoreCache.put(cacheKey, new CachedScore(finalScore, System.currentTimeMillis()));

            logger.debug("Score calculé pour {} : popularité={}, fraîcheur={}, localisation={}, total={}",
                    document.getUrl(), popularityScore, freshnessScore, locationScore, finalScore);

            return finalScore;

        } catch (Exception e) {
            logger.error("Échec du calcul du score pour le document : {}", document.getUrl(), e);
            return 0.0;
        }
    }

    private String generateCacheKey(DocumentResponse document, SearchContext context,
                                    double popularityWeight, double freshnessWeight, double locationWeight) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(document.getUrl())
                .append("|").append(context != null ? context.hashCode() : 0)
                .append("|").append(popularityWeight)
                .append("|").append(freshnessWeight)
                .append("|").append(locationWeight);

        return String.valueOf(keyBuilder.toString().hashCode());
    }

    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        scoreCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > cacheTtl);

        logger.debug("Cache nettoyé, {} entrées restantes", scoreCache.size());
    }

    private static class CachedScore {
        final double score;
        final long timestamp;

        CachedScore(double score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300000; // 5 minutes
        }
    }
}
