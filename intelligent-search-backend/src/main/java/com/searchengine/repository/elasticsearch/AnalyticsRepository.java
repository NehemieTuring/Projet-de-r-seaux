package com.searchengine.repository.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.searchengine.model.entity.SearchAnalytics;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository pour gérer les données d'analyse dans Elasticsearch.
 */
@Repository
public class AnalyticsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsRepository.class);
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public AnalyticsRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * Sauvegarde une entité SearchAnalytics dans Elasticsearch.
     *
     * @param analytics Entité à sauvegarder
     * @return Entité sauvegardée
     */
    public SearchAnalytics save(SearchAnalytics analytics) {
        logger.debug("Sauvegarde des données d'analyse : {}", analytics.getAnalyticsId());
        try {
            elasticsearchClient.index(i -> i
                    .index("analytics")
                    .id(analytics.getAnalyticsId())
                    .document(analytics));
            return analytics;
        } catch (IOException e) {
            logger.error("Échec de la sauvegarde des données d'analyse : {}", analytics.getAnalyticsId(), e);
            throw new RuntimeException("Échec de la sauvegarde des données d'analyse", e);
        }
    }

    /**
     * Recherche des analyses par query.
     *
     * @param query Texte de recherche
     * @return Liste des analyses correspondantes
     */
    public List<SearchAnalytics> findByQueryContaining(String query) {
        logger.debug("Recherche des analyses avec query : {}", query);
        try {
            SearchResponse<SearchAnalytics> response = elasticsearchClient.search(s -> s
                            .index("analytics")
                            .query(q -> q
                                    .match(m -> m
                                            .field("description")
                                            .query(query))),
                    SearchAnalytics.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Échec de la recherche des analyses avec query : {}", query, e);
            return Collections.emptyList();
        }
    }

    public List<SearchAnalytics> findByTimestampBetween(@NotNull(message = "Start time cannot be null") Instant startTime, @NotNull(message = "End time cannot be null") Instant endTime) {
        return List.of();
    }
}