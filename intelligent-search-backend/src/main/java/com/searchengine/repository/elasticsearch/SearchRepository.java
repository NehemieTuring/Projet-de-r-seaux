package com.searchengine.repository.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.searchengine.model.entity.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository pour gérer les requêtes de recherche dans Elasticsearch.
 */
@Repository
public class SearchRepository {

    private static final Logger logger = LoggerFactory.getLogger(SearchRepository.class);
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public SearchRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * Sauvegarde une entité SearchQuery dans Elasticsearch.
     *
     * @param searchQuery Entité à sauvegarder
     * @return Entité sauvegardée
     */
    public SearchQuery save(SearchQuery searchQuery) {
        logger.debug("Sauvegarde de la requête de recherche : {}", searchQuery.getQueryId());
        try {
            elasticsearchClient.index(i -> i
                    .index("search_queries")
                    .id(searchQuery.getQueryId())
                    .document(searchQuery));
            return searchQuery;
        } catch (IOException e) {
            logger.error("Échec de la sauvegarde de la requête de recherche : {}", searchQuery.getQueryId(), e);
            throw new RuntimeException("Échec de la sauvegarde de la requête de recherche", e);
        }
    }

    /**
     * Recherche des requêtes par texte.
     *
     * @param query Texte de recherche
     * @return Liste des requêtes correspondantes
     */
    public List<SearchQuery> findByQueryContaining(String query) {
        logger.debug("Recherche des requêtes avec query : {}", query);
        try {
            SearchResponse<SearchQuery> response = elasticsearchClient.search(s -> s
                            .index("search_queries")
                            .query(q -> q
                                    .match(m -> m
                                            .field("description")
                                            .query(query))),
                    SearchQuery.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Échec de la recherche des requêtes avec query : {}", query, e);
            return Collections.emptyList();
        }
    }
}