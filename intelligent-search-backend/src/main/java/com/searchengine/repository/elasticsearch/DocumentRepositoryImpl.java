package com.searchengine.repository.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchengine.model.entity.SearchDocument;
import com.searchengine.model.enums.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DocumentRepositoryImpl implements DocumentRepositoryCustom {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX_NAME = "documents";

    @Override
    public Page<SearchDocument> findByContentContainingOrTitleContaining(
            String content, String title, Pageable pageable) {

        String cleanContent = sanitizeSearchTerm(content);
        String cleanTitle = sanitizeSearchTerm(title);

        if (!StringUtils.hasText(cleanContent) && !StringUtils.hasText(cleanTitle)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildContainsQuery(cleanContent, cleanTitle, null, null);
        return executeSearch(searchQuery, pageable);
    }

    @Override
    public Page<SearchDocument> findByContentContainingOrTitleContainingAndDocumentType(
            String content, String title, DocumentType documentType, Pageable pageable) {

        String cleanContent = sanitizeSearchTerm(content);
        String cleanTitle = sanitizeSearchTerm(title);

        if (!StringUtils.hasText(cleanContent) && !StringUtils.hasText(cleanTitle)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildContainsQuery(cleanContent, cleanTitle, documentType, null);
        return executeSearch(searchQuery, pageable);
    }

    @Override
    public Page<SearchDocument> findByContentContainingOrTitleContainingAndLanguage(
            String content, String title, String language, Pageable pageable) {

        String cleanContent = sanitizeSearchTerm(content);
        String cleanTitle = sanitizeSearchTerm(title);

        if (!StringUtils.hasText(cleanContent) && !StringUtils.hasText(cleanTitle)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildContainsQuery(cleanContent, cleanTitle, null, language);
        return executeSearch(searchQuery, pageable);
    }

    @Override
    public Page<SearchDocument> findByContentContainingOrTitleContainingAndDocumentTypeAndLanguage(
            String content, String title, DocumentType documentType, String language, Pageable pageable) {

        String cleanContent = sanitizeSearchTerm(content);
        String cleanTitle = sanitizeSearchTerm(title);

        if (!StringUtils.hasText(cleanContent) && !StringUtils.hasText(cleanTitle)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildContainsQuery(cleanContent, cleanTitle, documentType, language);
        return executeSearch(searchQuery, pageable);
    }

    public Page<SearchDocument> findByTitleAutocomplete(String prefix, String language, Pageable pageable) {
        if (!StringUtils.hasText(prefix)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query query = Query.of(q -> q.matchPhrasePrefix(m -> m
                .field("title")
                .query(prefix)
        ));

        if (StringUtils.hasText(language)) {
            Query finalQuery = query;
            query = Query.of(q -> q.bool(b -> b
                    .must(finalQuery)
                    .filter(f -> f.term(t -> t
                            .field("metadata.language")
                            .value(language)
                    ))
            ));
        }

        return executeSearch(query, pageable);
    }

    @Override
    public Page<SearchDocument> findByTitleStartingWithOrContentStartingWith(
            String title, String content, Pageable pageable) {

        String cleanTitle = sanitizeSearchTerm(title);
        String cleanContent = sanitizeSearchTerm(content);

        if (!StringUtils.hasText(cleanTitle) && !StringUtils.hasText(cleanContent)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildPrefixQuery(cleanTitle, cleanContent, null);
        return executeSearch(searchQuery, pageable);
    }

    @Override
    public Page<SearchDocument> findByTitleStartingWithOrContentStartingWithAndLanguage(
            String title, String content, String language, Pageable pageable) {

        String cleanTitle = sanitizeSearchTerm(title);
        String cleanContent = sanitizeSearchTerm(content);

        if (!StringUtils.hasText(cleanTitle) && !StringUtils.hasText(cleanContent)) {
            log.warn("Termes de recherche vides après nettoyage");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Query searchQuery = buildPrefixQuery(cleanTitle, cleanContent, language);
        return executeSearch(searchQuery, pageable);
    }

    private String sanitizeSearchTerm(String term) {
        if (!StringUtils.hasText(term)) {
            return "";
        }

        String cleaned = term.trim()
                .replaceAll("[*+\\-!(){}\\[\\]^~?:\\\\/]", " ")
                .replaceAll("[\"“”]", " ")
                .replaceAll("[‘’']", " ")
                .replaceAll("\\s+", " ")
                .trim();

        log.debug("Terme nettoyé: '{}' -> '{}'", term, cleaned);
        return cleaned;
    }

    private Query buildContainsQuery(String cleanContent, String cleanTitle,
                                     DocumentType documentType, String language) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        BoolQuery.Builder shouldQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(cleanContent)) {
            shouldQueryBuilder.should(Query.of(q -> q
                    .match(m -> m
                            .field("content")
                            .query(cleanContent)
                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                    )
            ));
        }

        if (StringUtils.hasText(cleanTitle)) {
            shouldQueryBuilder.should(Query.of(q -> q
                    .match(m -> m
                            .field("title")
                            .query(cleanTitle)
                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                    )
            ));
        }

        boolQueryBuilder.must(Query.of(q -> q.bool(shouldQueryBuilder.minimumShouldMatch("1").build())));

        if (documentType != null) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t
                            .field("documentType")
                            .value(documentType.getValue())
                    )
            ));
        }

        if (StringUtils.hasText(language)) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t
                            .field("metadata.language")
                            .value(language)
                    )
            ));
        }

        return Query.of(q -> q.bool(boolQueryBuilder.build()));
    }

    private Query buildPrefixQuery(String cleanTitle, String cleanContent, String language) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        BoolQuery.Builder shouldQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(cleanTitle)) {
            shouldQueryBuilder.should(Query.of(q -> q
                    .prefix(p -> p
                            .field("title")
                            .value(cleanTitle)
                    )
            ));
        }

        if (StringUtils.hasText(cleanContent)) {
            shouldQueryBuilder.should(Query.of(q -> q
                    .prefix(p -> p
                            .field("content")
                            .value(cleanContent)
                    )
            ));
        }

        boolQueryBuilder.must(Query.of(q -> q.bool(shouldQueryBuilder.minimumShouldMatch("1").build())));

        if (StringUtils.hasText(language)) {
            boolQueryBuilder.filter(Query.of(q -> q
                    .term(t -> t
                            .field("metadata.language")
                            .value(language)
                    )
            ));
        }

        return Query.of(q -> q.bool(boolQueryBuilder.build()));
    }

    private Page<SearchDocument> executeSearch(Query query, Pageable pageable) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(query)
                    .from((int) pageable.getOffset())
                    .size(pageable.getPageSize())
                    .source(src -> src.filter(f -> f.includes(
                            "url", "title", "content", "description", "documentType",
                            "links", "mediaUrls", "apiEndpoints", "isDynamic",
                            "crawlTimestamp", "httpStatus", "metadata", "contentHash"
                    )))
            );

            // Débogage : Inspecter la réponse brute
            SearchResponse<Map> rawResponse = elasticsearchClient.search(searchRequest, Map.class);
            log.debug("Réponse brute Elasticsearch : {}", rawResponse.hits().hits());

            // Inspecter chaque document pour identifier les champs problématiques
            rawResponse.hits().hits().forEach(hit -> {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    log.debug("Document source : {}", source);
                    log.debug("documentType : {}", source.get("documentType"));
                    log.debug("apiEndpoints : {}", source.get("apiEndpoints"));
                    log.debug("crawlTimestamp : {}", source.get("crawlTimestamp"));
                    log.debug("metadata : {}", source.get("metadata"));
                }
            });

            // Désérialisation en SearchDocument
            SearchResponse<SearchDocument> response = elasticsearchClient.search(searchRequest, SearchDocument.class);

            List<SearchDocument> documents = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .collect(Collectors.toList());

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

            return new PageImpl<>(documents, pageable, totalHits);

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la requête Elasticsearch : {}", e.getMessage(), e);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
}