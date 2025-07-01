package com.searchengine.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.searchengine.utils.ElasticsearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Component responsible for initializing Elasticsearch indices.
 * Separated from ElasticsearchConfig to avoid circular dependencies.
 */
@Component
public class ElasticsearchInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchInitializer.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * Initialise les index Elasticsearch avec les mappings et paramètres.
     * Utilise @EventListener pour s'assurer que tous les beans sont créés.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initIndices() {
        try {
            initializeIndex("documents", "elasticsearch/mappings/document-mapping.json");
            initializeIndex("analytics", "elasticsearch/mappings/analytics-mapping.json");
            initializeIndex("search_queries", "elasticsearch/mappings/search_queries-mapping.json");
            logger.info("Index Elasticsearch initialisés avec succès");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des index Elasticsearch", e);
            throw new IllegalStateException("Échec de l'initialisation des index Elasticsearch", e);
        }
    }

    /**
     * Initialise un index Elasticsearch unique avec le fichier de mapping spécifié.
     *
     * @param indexName Nom de l'index
     * @param mappingFile Nom du fichier de mapping dans les ressources
     */
    private void initializeIndex(String indexName, String mappingFile) {
        try {
            String mapping = Files.readString(Paths.get("src/main/resources/" + mappingFile));
            if (!ElasticsearchUtils.indexExists(elasticsearchClient, indexName)) {
                ElasticsearchUtils.createIndex(elasticsearchClient, indexName, mapping);
                logger.info("Index Elasticsearch créé : {}", indexName);
            } else {
                logger.debug("L'index Elasticsearch '{}' existe déjà", indexName);
            }
        } catch (IOException e) {
            logger.error("Échec de la lecture du fichier de mapping : {}", mappingFile, e);
            throw new IllegalStateException("Échec de la lecture du fichier de mapping : " + mappingFile, e);
        }
    }
}