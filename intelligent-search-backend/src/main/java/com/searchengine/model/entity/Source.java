package com.searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entity representing a data source for crawling.
 */
@Data
@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastCrawledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}