package com.searchengine.service;

import com.searchengine.component.indexing.DocumentProcessor;
import com.searchengine.model.dto.request.IndexRequest;
import com.searchengine.model.entity.SearchDocument;
import com.searchengine.repository.elasticsearch.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for processing indexing requests from crawler agents.
 */
@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private final DocumentRepository documentRepository;
    private final DocumentProcessor documentProcessor;
    private final ElasticsearchOperations elasticsearchOperations;
    private final int batchSize;
    private final int subBatchSize;

    public IndexingService(DocumentRepository documentRepository,
                           DocumentProcessor documentProcessor,
                           ElasticsearchOperations elasticsearchOperations,
                           @Value("${app.indexing.batch-size:100}") int batchSize,
                           @Value("${app.indexing.sub-batch-size:20}") int subBatchSize) {
        this.documentRepository = documentRepository;
        this.documentProcessor = documentProcessor;
        this.elasticsearchOperations = elasticsearchOperations;
        this.batchSize = batchSize;
        this.subBatchSize = Math.max(1, subBatchSize); // Éviter les valeurs <= 0
    }

    /**
     * Processes an indexing request by validating and indexing documents.
     *
     * @param indexRequest The indexing request containing documents
     */
    public void processIndexRequest(IndexRequest indexRequest) {
        logger.info("Processing index request with batchId: {}", indexRequest.getBatchId());
        List<SearchDocument> searchDocuments = indexRequest.getResults();

        // Vérification initiale
        if (searchDocuments == null || searchDocuments.isEmpty()) {
            logger.warn("Empty document list for batchId: {}", indexRequest.getBatchId());
            return;
        }

        if (searchDocuments.size() > batchSize) {
            logger.warn("Index request batch size {} exceeds configured limit {}", searchDocuments.size(), batchSize);
            throw new IllegalArgumentException("Batch size exceeds limit: " + batchSize);
        }

        // Traitement et validation des documents
        List<SearchDocument> validSearchDocuments = new ArrayList<>();
        List<String> processingErrors = new ArrayList<>();

        for (SearchDocument doc : searchDocuments) {
            try {
                documentProcessor.processDocument(doc);
                validSearchDocuments.add(doc);
            } catch (Exception e) {
                logger.error("Error processing document {}, skipping it", doc.getUrl(), e);
                processingErrors.add(doc.getUrl());
            }
        }

        if (validSearchDocuments.isEmpty()) {
            logger.warn("No valid documents to index for batchId: {}", indexRequest.getBatchId());
            return;
        }

        // Indexation en sous-lots
        List<String> indexingErrors = new ArrayList<>();
        int indexedCount = 0;

        try {
            for (int i = 0; i < validSearchDocuments.size(); i += subBatchSize) {
                int endIndex = Math.min(i + subBatchSize, validSearchDocuments.size());
                List<SearchDocument> subBatch = validSearchDocuments.subList(i, endIndex);

                // Préparer les IndexQuery
                List<IndexQuery> indexQueries = subBatch.stream()
                        .map(doc -> new IndexQueryBuilder()
                                .withId(doc.getUrl())
                                .withObject(doc)
                                .build())
                        .collect(Collectors.toList());

                try {
                    // Indexation bulk
                    List<IndexedObjectInformation> bulkResponse = elasticsearchOperations.bulkIndex(
                            indexQueries, IndexCoordinates.of("documents"));

                    // Aucun moyen de savoir si un document a échoué individuellement, donc on considère tout comme indexé
                    indexedCount += bulkResponse.size();

                } catch (Exception e) {
                    logger.error("Erreur pendant l'indexation d'un sous-lot (taille = {}): {}", subBatch.size(), e.getMessage(), e);
                    // En cas d'erreur globale du sous-lot, on log les documents
                    subBatch.forEach(doc -> indexingErrors.add(doc.getUrl()));
                }
            }

            // Résumé des résultats
            logger.info("Successfully indexed {} out of {} documents for batchId: {}",
                    indexedCount, validSearchDocuments.size(), indexRequest.getBatchId());

            if (!processingErrors.isEmpty() || !indexingErrors.isEmpty()) {
                logger.warn("Indexing summary for batchId: {} - Processing errors: {}, Indexing errors: {}",
                        indexRequest.getBatchId(), processingErrors.size(), indexingErrors.size());
            }

        } catch (Exception e) {
            logger.error("Failed to index documents for batchId: {}", indexRequest.getBatchId(), e);
            throw new RuntimeException("Indexing failed for batchId: " + indexRequest.getBatchId(), e);
        }
    }
}