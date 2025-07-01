package com.searchengine.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ElasticsearchUtils {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUtils.class);

    /**
     * Vérifie si un index existe dans Elasticsearch.
     *
     * @param client Client Elasticsearch
     * @param indexName Nom de l'index
     * @return true si l'index existe, false sinon
     */
    public static boolean indexExists(ElasticsearchClient client, String indexName) throws IOException {
        ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
        return client.indices().exists(request).value();
    }

    /**
     * Crée un index Elasticsearch avec le mapping spécifié.
     *
     * @param client Client Elasticsearch
     * @param indexName Nom de l'index
     * @param mapping JSON du mapping
     */
    public static void createIndex(ElasticsearchClient client, String indexName, String mapping) throws IOException {
        CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(indexName)
                .withJson(new java.io.StringReader(mapping))
        );
        client.indices().create(request);
    }
}