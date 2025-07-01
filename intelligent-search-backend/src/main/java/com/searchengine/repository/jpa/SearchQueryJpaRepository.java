package com.searchengine.repository.jpa;

import com.searchengine.model.entity.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchQueryJpaRepository extends JpaRepository<SearchQuery, Long> {
    List<SearchQuery> findByQueryContainingIgnoreCase(String query);
}