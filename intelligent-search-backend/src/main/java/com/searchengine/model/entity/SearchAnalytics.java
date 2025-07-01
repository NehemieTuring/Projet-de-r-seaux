package com.searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entité représentant les données d'analyse pour la recherche et l'indexation.
 */
@Data
@Entity
@Table(name = "search_analytics")
public class SearchAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String analyticsId;

    private String description;

    // Add the query field that was missing
    private String query;

    private int resultCount;

    private String sessionId;

    private LocalDateTime timestamp;

    private String actionType; // e.g., SEARCH, INDEX

    @Embedded
    private Metadata metadata;

    /**
     * Classe interne pour les métadonnées supplémentaires.
     */
    @Data
    @Embeddable
    public static class Metadata {
        private String ipAddress;
        private String userAgent;
        private String status; // e.g., SUCCESS, ERROR
    }
}