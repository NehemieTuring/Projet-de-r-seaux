package com.searchengine.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.searchengine.component.context.SessionTracker;
import com.searchengine.component.scoring.ScoreCalculator;
import com.searchengine.exception.SearchException;
import com.searchengine.model.dto.request.SearchRequestDto;
import com.searchengine.model.dto.response.SearchResponseDto;
import com.searchengine.model.dto.response.DocumentResponse;
import com.searchengine.model.entity.SearchContext;
import com.searchengine.model.entity.SearchDocument;
import com.searchengine.utils.SearchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ScoreCalculator scoreCalculator;
    private final ContextService contextService;
    private final SessionTracker sessionTracker;

    @Value("${app.scoring.weight.popularity:0.4}")
    private double popularityWeight;

    @Value("${app.scoring.weight.freshness:0.3}")
    private double freshnessWeight;

    @Value("${app.scoring.weight.relevance:0.3}")
    private double relevanceWeight;

    @Value("${app.scoring.weight.location:0.0}")
    private double locationWeight;

    @Value("${app.scoring.weight.languageBoost:1.2}")
    private double languageBoost;

    @Value("${app.search.fuzziness:AUTO}")
    private String fuzzinessLevel; // Nouvelle propriété pour configurer la fuzziness

    @Autowired
    public SearchService(ElasticsearchClient elasticsearchClient, ScoreCalculator scoreCalculator,
                         ContextService contextService, SessionTracker sessionTracker) {
        this.elasticsearchClient = elasticsearchClient;
        this.scoreCalculator = scoreCalculator;
        this.contextService = contextService;
        this.sessionTracker = sessionTracker;
    }

    public SearchResponseDto search(SearchRequestDto request, String ipAddress, String userAgent) {
        log.info("Traitement de la requête de recherche : {}", request.getQuery());
        try {
            String sessionId = sessionTracker.getOrCreateSessionId(ipAddress);
            SearchContext context = contextService.buildContext(sessionId, ipAddress, userAgent);

            String cleanedQuery = SearchUtils.cleanQuery(request.getQuery()).trim();

            if (!StringUtils.hasText(cleanedQuery) || cleanedQuery.contains("*") || cleanedQuery.contains("\"")) {
                throw new SearchException("Requête invalide ou dangereuse");
            }

            // Construction de la requête Elasticsearch
            SearchRequest esRequest = SearchRequest.of(s -> s
                    .index("documents")
                    .size(request.getSize())
                    .query(q -> q.bool(b -> {
                        // Remplacement de queryString par match avec fuzziness
                        b.must(m -> m.match(mq -> mq
                                .field("content")
                                .query(cleanedQuery)
                                .fuzziness(fuzzinessLevel) // Ajout de la fuzziness
                                .prefixLength(1) // Protège les premiers caractères pour performances
                                .maxExpansions(50) // Limite l'expansion pour éviter surcharge
                        ));

                        // Filtre par type de document uniquement si spécifié
                        if (request.getDocumentType() != null && request.getDocumentType().getValue() != null) {
                            b.filter(f -> f.term(t -> t.field("documentType").value(request.getDocumentType().getValue())));
                        }

                        // Filtre par langue si spécifié
                        if (StringUtils.hasText(request.getLanguage())) {
                            b.filter(f -> f.term(t -> t.field("metadata.language").value(request.getLanguage())));
                        }
                        return b;
                    }))
                    .source(src -> src.filter(f -> f.includes("title", "url", "description", "documentType",
                            "crawlTimestamp", "links", "mediaUrls", "metadata")))
            );

            // Exécution de la requête
            SearchResponse<SearchDocument> esResponse = elasticsearchClient.search(esRequest, SearchDocument.class);
            List<SearchDocument> searchDocuments = esResponse.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Conversion en DocumentResponse
            List<DocumentResponse> results = searchDocuments.stream().map(doc -> {
                DocumentResponse resp = new DocumentResponse();
                resp.setUrl(doc.getUrl());
                resp.setTitle(doc.getTitle());
                resp.setDescription(doc.getDescription());
                resp.setDocumentType(doc.getDocumentType());
                resp.setCrawlTimestamp(doc.getCrawlTimestamp());
                resp.setLinks(doc.getLinks());
                resp.setMediaUrls(doc.getMediaUrls());
                resp.setMetadata(doc.getMetadata());
                return resp;
            }).collect(Collectors.toList());

            // Calcul des scores
            for (DocumentResponse doc : results) {
                double relevanceScore = 1.0;
                double languageBoostValue = 1.0;
                if (StringUtils.hasText(request.getLanguage()) &&
                        request.getLanguage().equalsIgnoreCase(doc.getMetadata().getOrDefault("language", ""))) {
                    languageBoostValue = this.languageBoost;
                }

                double otherScore = scoreCalculator.calculateScore(doc, context,
                        popularityWeight, freshnessWeight, locationWeight);

                double finalScore = (
                        otherScore * (popularityWeight + freshnessWeight + locationWeight) +
                                relevanceScore * relevanceWeight
                ) / (popularityWeight + freshnessWeight + locationWeight + relevanceWeight) * languageBoostValue;

                doc.setScore(finalScore);
            }

            // Tri des résultats par score
            List<DocumentResponse> sortedResults = results.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .collect(Collectors.toList());

            // Construction de la réponse
            SearchResponseDto response = new SearchResponseDto();
            response.setResults(sortedResults);
            response.setTotalResults(esResponse.hits().total() != null ? esResponse.hits().total().value() : 0);
            response.setContext(context);

            return response;

        } catch (IOException e) {
            log.error("Échec de la requête Elasticsearch", e);
            throw new SearchException("Erreur d'accès aux données Elasticsearch", e);
        } catch (Exception e) {
            log.error("Échec de la recherche", e);
            throw new SearchException("Échec de l'opération de recherche", e);
        }
    }
}