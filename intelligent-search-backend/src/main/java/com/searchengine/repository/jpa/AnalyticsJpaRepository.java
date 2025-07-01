package com.searchengine.repository.jpa;

import com.searchengine.model.entity.SearchAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for search analytics data in PostgreSQL.
 */
@Repository
public interface AnalyticsJpaRepository extends JpaRepository<SearchAnalytics, Long> {

    /**
     * Finds analytics records within a time range.
     * Updated to use LocalDateTime to match the entity field type.
     *
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of matching analytics records
     */
    List<SearchAnalytics> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Option 1: If you add query field to SearchAnalytics entity
     */
    List<SearchAnalytics> findByQueryContainingIgnoreCase(String query);

    /**
     * Option 2: Alternative - search by description instead of query
     */
    List<SearchAnalytics> findByDescriptionContainingIgnoreCase(String description);
}