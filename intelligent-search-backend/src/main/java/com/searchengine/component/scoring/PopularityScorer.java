package com.searchengine.component.scoring;

import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composant optimisé pour calculer les scores de popularité avec algorithmes avancés.
 */
@Component
public class PopularityScorer {

    private static final Logger logger = LoggerFactory.getLogger(PopularityScorer.class);

    @Value("${app.scoring.popularity.max-score:100.0}")
    private double maxPopularityScore;

    @Value("${app.scoring.popularity.default-score:50.0}")
    private double defaultPopularityScore;

    @Value("${app.scoring.popularity.link-weight:2.0}")
    private double linkWeight;

    @Value("${app.scoring.popularity.media-weight:3.0}")
    private double mediaWeight;

    @Value("${app.scoring.popularity.domain-authority-weight:5.0}")
    private double domainAuthorityWeight;

    // Cache pour les scores de domaine
    private final Map<String, Double> domainAuthorityCache = new ConcurrentHashMap<>();

    /**
     * Calcule le score de popularité avec algorithmes sophistiqués.
     */
    public double calculatePopularityScore(DocumentResponse document) {
        logger.debug("Calcul du score de popularité pour le document: {}", document.getUrl());
        try {
            // Calcul multi-factoriel de la popularité
            PopularityFactors factors = extractPopularityFactors(document);

            // Score composite basé sur plusieurs métriques
            double compositeScore = calculateCompositePopularityScore(factors, document);

            // Normalisation avec fonction de saturation
            double normalizedScore = applySaturationFunction(compositeScore);

            logger.debug("Score de popularité pour {} : {} (facteurs: {})",
                    document.getUrl(), normalizedScore, factors);
            return normalizedScore;

        } catch (Exception e) {
            logger.error("Échec du calcul du score de popularité pour le document: {}", document.getUrl(), e);
            return defaultPopularityScore / maxPopularityScore; // Score par défaut normalisé
        }
    }

    /**
     * Surcharge pour Document entity.
     */
    public double calculatePopularityScore(SearchDocument searchDocument) {
        logger.debug("Calcul du score de popularité pour le document: {}", searchDocument.getUrl());
        try {
            PopularityFactors factors = extractPopularityFactors(searchDocument);
            double compositeScore = calculateCompositePopularityScore(factors, searchDocument);
            double normalizedScore = applySaturationFunction(compositeScore);

            logger.debug("Score de popularité pour {} : {} (facteurs: {})",
                    searchDocument.getUrl(), normalizedScore, factors);
            return normalizedScore;

        } catch (Exception e) {
            logger.error("Échec du calcul du score de popularité pour le document: {}", searchDocument.getUrl(), e);
            return defaultPopularityScore / maxPopularityScore;
        }
    }

    /**
     * Extrait les facteurs de popularité d'un DocumentResponse.
     */
    private PopularityFactors extractPopularityFactors(DocumentResponse document) {
        PopularityFactors factors = new PopularityFactors();

        // Extraction depuis les métadonnées
        if (document.getMetadata() != null) {
            Map<String, String> metadata = document.getMetadata();

            // Score de popularité explicite (si disponible)
            String popularityStr = metadata.get("popularity");
            if (popularityStr != null) {
                try {
                    factors.explicitPopularity = Double.parseDouble(popularityStr);
                } catch (NumberFormatException e) {
                    logger.warn("Métadonnée de popularité invalide: {}", popularityStr);
                }
            }

            // Compteurs de liens et médias
            factors.linkCount = parseIntMetadata(metadata, "link_count", 0);
            factors.mediaCount = parseIntMetadata(metadata, "media_count", 0);
            factors.inboundLinks = parseIntMetadata(metadata, "inbound_links", 0);
            factors.shareCount = parseIntMetadata(metadata, "share_count", 0);
            factors.commentCount = parseIntMetadata(metadata, "comment_count", 0);

            // Métriques de qualité du contenu
            factors.contentLength = parseIntMetadata(metadata, "content_length", 0);
            factors.imageCount = parseIntMetadata(metadata, "image_count", 0);
        }

        // Extraction depuis les champs directs (si disponibles dans DocumentResponse)
        if (document.getTitle() != null) {
            factors.titleLength = document.getTitle().length();
        }
        if (document.getDescription() != null) {
            factors.descriptionLength = document.getDescription().length();
        }

        return factors;
    }

    /**
     * Extrait les facteurs de popularité d'un Document entity.
     */
    private PopularityFactors extractPopularityFactors(SearchDocument searchDocument) {
        PopularityFactors factors = new PopularityFactors();

        // Compteurs directs
        factors.linkCount = searchDocument.getLinks() != null ? searchDocument.getLinks().size() : 0;
        factors.mediaCount = searchDocument.getMediaUrls() != null ? searchDocument.getMediaUrls().size() : 0;

        // Extraction depuis les métadonnées (si disponibles)
        if (searchDocument.getMetadata() != null) {
            factors.inboundLinks = parseIntMetadata(searchDocument.getMetadata(), "inbound_links", 0);
            factors.shareCount = parseIntMetadata(searchDocument.getMetadata(), "share_count", 0);
            factors.commentCount = parseIntMetadata(searchDocument.getMetadata(), "comment_count", 0);
        }

        // Métriques de contenu
        if (searchDocument.getTitle() != null) {
            factors.titleLength = searchDocument.getTitle().length();
        }
        if (searchDocument.getContent() != null) {
            factors.contentLength = searchDocument.getContent().length();
        }

        return factors;
    }

    /**
     * Calcule le score composite basé sur tous les facteurs.
     */
    private double calculateCompositePopularityScore(PopularityFactors factors, Object document) {
        double score = defaultPopularityScore;

        // Score explicite (priorité haute si disponible)
        if (factors.explicitPopularity > 0) {
            score = factors.explicitPopularity;
        }

        // Bonus basés sur les métriques d'engagement
        score += factors.linkCount * linkWeight;
        score += factors.mediaCount * mediaWeight;
        score += factors.inboundLinks * 1.5; // Liens entrants sont très importants
        score += factors.shareCount * 2.0;
        score += factors.commentCount * 1.0;

        // Bonus de qualité du contenu
        score += calculateContentQualityBonus(factors);

        // Bonus d'autorité du domaine
        String domain = extractDomain(getDocumentUrl(document));
        if (domain != null) {
            score += getDomainAuthorityScore(domain) * domainAuthorityWeight;
        }

        return score;
    }

    /**
     * Calcule le bonus de qualité du contenu.
     */
    private double calculateContentQualityBonus(PopularityFactors factors) {
        double bonus = 0.0;

        // Bonus pour contenu substantiel
        if (factors.contentLength > 1000) {
            bonus += 5.0; // Contenu long généralement de meilleure qualité
        } else if (factors.contentLength > 500) {
            bonus += 2.0;
        }

        // Bonus pour titre optimisé
        if (factors.titleLength >= 30 && factors.titleLength <= 60) {
            bonus += 2.0; // Longueur de titre optimale pour le SEO
        }

        // Bonus pour richesse multimédia
        if (factors.imageCount > 0) {
            bonus += Math.min(3.0, factors.imageCount * 0.5);
        }

        return bonus;
    }

    /**
     * Applique une fonction de saturation pour éviter les scores extrêmes.
     */
    private double applySaturationFunction(double rawScore) {
        // Fonction sigmoïde pour normalisation douce
        double normalized = rawScore / maxPopularityScore;

        // Application d'une fonction de saturation
        double saturated = 2.0 / (1.0 + Math.exp(-2.0 * normalized)) - 1.0;

        return Math.max(0.0, Math.min(1.0, saturated));
    }

    /**
     * Calcule ou récupère le score d'autorité du domaine.
     */
    private double getDomainAuthorityScore(String domain) {
        // Utilisation du cache pour éviter les recalculs
        return domainAuthorityCache.computeIfAbsent(domain, this::calculateDomainAuthority);
    }

    /**
     * Calcule l'autorité d'un domaine (algorithme simplifié).
     */
    private double calculateDomainAuthority(String domain) {
        // Algorithme simplifié basé sur des heuristiques
        double authority = 1.0; // Score de base

        // Bonus pour domaines connus/populaires
        if (isWellKnownDomain(domain)) {
            authority += 8.0;
        }

        // Pénalité pour domaines suspects
        if (isSuspiciousDomain(domain)) {
            authority -= 3.0;
        }

        // Analyse de la structure du domaine
        authority += analyzeDomainStructure(domain);

        return Math.max(0.0, Math.min(10.0, authority));
    }

    /**
     * Vérifie si un domaine est bien connu.
     */
    private boolean isWellKnownDomain(String domain) {
        String[] wellKnownDomains = {
                "wikipedia.org", "github.com", "stackoverflow.com",
                "medium.com", "linkedin.com", "google.com"
        };

        for (String knownDomain : wellKnownDomains) {
            if (domain.contains(knownDomain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un domaine semble suspect.
     */
    private boolean isSuspiciousDomain(String domain) {
        // Heuristiques simples pour détecter des domaines suspects
        return domain.contains("spam") ||
                domain.contains("fake") ||
                domain.matches(".*\\d{4,}.*") || // Beaucoup de chiffres
                domain.split("\\.").length > 4; // Trop de sous-domaines
    }

    /**
     * Analyse la structure du domaine pour des indices de qualité.
     */
    private double analyzeDomainStructure(String domain) {
        double bonus = 0.0;

        // Bonus pour TLD de qualité
        if (domain.endsWith(".edu") || domain.endsWith(".gov")) {
            bonus += 3.0;
        } else if (domain.endsWith(".org")) {
            bonus += 1.0;
        }

        // Pénalité pour domaines très longs
        if (domain.length() > 50) {
            bonus -= 1.0;
        }

        return bonus;
    }

    // Méthodes utilitaires
    private int parseIntMetadata(Map<String, String> metadata, String key, int defaultValue) {
        try {
            String value = metadata.get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String extractDomain(String url) {
        try {
            if (url == null) return null;
            return url.replaceAll("https?://", "")
                    .replaceAll("www\\.", "")
                    .split("/")[0]
                    .toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private String getDocumentUrl(Object document) {
        if (document instanceof DocumentResponse) {
            return ((DocumentResponse) document).getUrl();
        } else if (document instanceof SearchDocument) {
            return ((SearchDocument) document).getUrl();
        }
        return null;
    }

    /**
     * Classe interne pour organiser les facteurs de popularité.
     */
    private static class PopularityFactors {
        double explicitPopularity = 0.0;
        int linkCount = 0;
        int mediaCount = 0;
        int inboundLinks = 0;
        int shareCount = 0;
        int commentCount = 0;
        int contentLength = 0;
        int titleLength = 0;
        int descriptionLength = 0;
        int imageCount = 0;

        @Override
        public String toString() {
            return String.format("PopularityFactors{explicit=%.1f, links=%d, media=%d, inbound=%d, shares=%d, comments=%d}",
                    explicitPopularity, linkCount, mediaCount, inboundLinks, shareCount, commentCount);
        }
    }
}