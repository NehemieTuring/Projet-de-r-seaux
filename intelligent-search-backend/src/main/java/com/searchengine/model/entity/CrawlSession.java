package com.searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entity representing a crawl session for a data source.
 */
@Data
@Entity
@Table(name = "crawl_sessions")
public class CrawlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column
    private Instant endedAt;

    @Column
    private Long documentsCrawled;

    @Column
    private String status; // e.g., RUNNING, COMPLETED, FAILED

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
        status = "RUNNING";
    }
}