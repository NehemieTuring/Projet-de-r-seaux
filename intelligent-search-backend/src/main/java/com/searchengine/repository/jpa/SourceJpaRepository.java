package com.searchengine.repository.jpa;

import com.searchengine.model.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for managing Source entities in PostgreSQL.
 */
@Repository
public interface SourceJpaRepository extends JpaRepository<Source, Long> {

    /**
     * Finds a source by its URL.
     *
     * @param url The source URL
     * @return Optional containing the source if found
     */
    Optional<Source> findByUrl(String url);
}