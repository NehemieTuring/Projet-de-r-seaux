package com.searchengine.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.searchengine.exception.SearchException;
import com.searchengine.model.dto.response.AutocompleteResponse;
import com.searchengine.model.entity.SearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AutocompleteService {

    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public AutocompleteService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public AutocompleteResponse getAutocompleteSuggestions(String prefix, String language, int maxSuggestions) {
        if (!StringUtils.hasText(prefix)) {
            log.warn("Préfixe vide fourni pour l'autocomplétion");
            return AutocompleteResponse.builder().suggestions(List.of()).build();
        }

        try {
            // Extraire le dernier mot partiel pour la recherche
            String trimmedPrefix = prefix.trim();
            String searchPrefix = extractLastWord(trimmedPrefix);
            log.debug("Préfixe de recherche extrait : {}", searchPrefix);

            // Restaurer la logique originale avec deux requêtes
            List<String> suggestions = searchElasticsearch(searchPrefix, maxSuggestions);
            List<SearchDocument> docs = fetchDocumentsFromElasticsearch(searchPrefix, maxSuggestions, language);

            log.debug("Suggestions from Elasticsearch (titles): {}", suggestions);
            log.debug("Fetched documents for completion: {} documents", docs.size());

            // Chercher une complétion dans les titres des suggestions
            for (String title : suggestions) {
                // Essayer d'abord une complétion pour le mot actuel
                String extension = findCompletionInText(title, searchPrefix);
                if (extension != null) {
                    log.debug("Found completion in title: {}", extension);
                    return buildResponse(suggestions, extension, true, maxSuggestions);
                }
                // Si aucune complétion n'est trouvée et le préfixe est vide (mot complet avec espace),
                // chercher le mot suivant
                if (!StringUtils.hasText(searchPrefix)) {
                    String nextWord = findNextWordInText(title, trimmedPrefix);
                    if (nextWord != null) {
                        log.debug("Found next word in title: {}", nextWord);
                        return buildResponse(suggestions, nextWord, false, maxSuggestions);
                    }
                }
            }

            // Si aucune complétion n'est trouvée, retourner sans complétion
            log.debug("No completion found for prefix: {}", searchPrefix);
            return buildResponse(suggestions, null, false, maxSuggestions);

        } catch (Exception e) {
            log.error("Erreur lors de l'autocomplétion pour le préfixe '{}'", prefix, e);
            throw new SearchException("Échec de l'opération d'autocomplétion", e);
        }
    }

    private AutocompleteResponse buildResponse(List<String> suggestions, String completion, boolean isExtension, int maxSuggestions) {
        return AutocompleteResponse.builder()
                .suggestions(suggestions.stream().distinct().limit(maxSuggestions).toList())
                .completion(completion)
                .isExtension(completion != null && isExtension)
                .build();
    }

    private List<String> searchElasticsearch(String prefix, int size) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index("documents")
                .size(size)
                .query(q -> q.bool(b -> b
                        .should(s1 -> s1.matchPhrasePrefix(mp -> mp.field("title").query(prefix)))
                        .should(s2 -> s2.matchPhrasePrefix(mp -> mp.field("content").query(prefix)))
                        .minimumShouldMatch("1")
                ))
                .source(src -> src.filter(f -> f.includes("title")))
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);

        return response.hits().hits().stream()
                .map(hit -> {
                    Map<String, Object> source = hit.source();
                    return source != null && source.containsKey("title")
                            ? source.get("title").toString()
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<SearchDocument> fetchDocumentsFromElasticsearch(String prefix, int size, String language) throws IOException {
        // Vérifier si la langue est fournie, sinon utiliser une valeur par défaut
        if (!StringUtils.hasText(language)) {
            log.warn("Langue non fournie, utilisation de la langue par défaut 'fr'");
            language = "fr";
        }

        String finalLanguage = language;
        SearchRequest request = SearchRequest.of(s -> s
                .index("documents")
                .size(size)
                .query(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("metadata.language").value(finalLanguage)))
                        .should(s1 -> s1.matchPhrasePrefix(mp -> mp.field("title").query(prefix)))
                        .should(s2 -> s2.matchPhrasePrefix(mp -> mp.field("content").query(prefix)))
                        .minimumShouldMatch("1")
                ))
                .source(src -> src.filter(f -> f.includes("title", "content", "metadata", "documentType", "apiEndpoints", "url", "description", "isDynamic", "crawlTimestamp", "httpStatus", "contentHash", "links", "mediaUrls")))
        );

        try {
            SearchResponse<Map> rawResponse = elasticsearchClient.search(request, Map.class);
            log.debug("Réponse brute Elasticsearch : {}", rawResponse.hits().hits());

            SearchResponse<SearchDocument> response = elasticsearchClient.search(request, SearchDocument.class);
            log.debug("Réponse Elasticsearch désérialisée : {}", response.hits().hits());
            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Échec de la désérialisation de la réponse Elasticsearch pour le préfixe '{}' et langue '{}'. Détails : {}",
                    prefix, language, e.getMessage());
            throw new IOException("Erreur lors de la récupération des documents", e);
        }
    }

    private String findCompletionInText(String text, String prefix) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(prefix)) return null;

        String[] words = text.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (StringUtils.hasText(word) && word.toLowerCase().startsWith(prefix.toLowerCase()) && word.length() > prefix.length()) {
                // Retourner uniquement la partie manquante du mot
                String completionPart = word.substring(prefix.length());
                log.debug("Found completion part: {}", completionPart);
                return completionPart;
            }
        }
        return null;
    }

    private String findNextWordInText(String text, String prefix) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(prefix)) return null;

        String lowerText = text.toLowerCase();
        String lowerPrefix = prefix.toLowerCase();
        int index = lowerText.indexOf(lowerPrefix);

        if (index < 0) return null;

        int after = index + lowerPrefix.length();

        // Sauter le mot actuel (lettres ou chiffres)
        while (after < lowerText.length() && Character.isLetterOrDigit(lowerText.charAt(after))) {
            after++;
        }
        // Sauter les espaces ou la ponctuation
        while (after < lowerText.length() && !Character.isLetterOrDigit(lowerText.charAt(after))) {
            after++;
        }

        int start = after;
        while (after < lowerText.length() && Character.isLetterOrDigit(lowerText.charAt(after))) {
            after++;
        }

        if (after > start) {
            // Retourner le mot suivant sans espace
            String nextWord = text.substring(start, after);
            log.debug("Found next word: {}", nextWord);
            return nextWord;
        }
        return null;
    }

    private String extractLastWord(String prefix) {
        if (!StringUtils.hasText(prefix)) return "";

        String[] parts = prefix.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}