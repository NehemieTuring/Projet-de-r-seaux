package com.searchengine.repository.jpa;

import com.searchengine.model.entity.SearchContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for managing SearchContext entities in PostgreSQL.
 */
@Repository
public interface ContextJpaRepository extends JpaRepository<SearchContext, String> {

    /**
     * Finds a search context by its session ID.
     *
     * @param sessionId The session ID
     * @return Optional containing the search context if found
     */
    Optional<SearchContext> findBySessionId(String sessionId);
}