package com.searchengine.model.entity;

import com.searchengine.model.enums.SearchType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entité représentant une requête de recherche exécutée par un utilisateur.
 */
@Data
@Entity
@Table(name = "search_queries")
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String queryId;

    // Add the actual search query text
    private String query;

    private String description;

    private String sessionId;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private SearchType searchType;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (queryId == null) {
            queryId = java.util.UUID.randomUUID().toString();
        }
    }
}