package com.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for search-related operations.
 */
public class SearchUtils {

    private static final Logger logger = LoggerFactory.getLogger(SearchUtils.class);

    // Pattern pour supprimer les caractères problématiques pour Elasticsearch
    private static final Pattern ELASTICSEARCH_SPECIAL_CHARS = Pattern.compile("[\"*+\\-!(){}\\[\\]^~?:\\\\/]");

    // Pattern pour nettoyer les caractères non alphanumériques (sauf espaces)
    private static final Pattern QUERY_CLEANUP_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");

    /**
     * Cleans a search query by removing special characters and normalizing whitespace.
     * Cette méthode est spécialement conçue pour éviter les erreurs Elasticsearch.
     *
     * @param query The raw search query
     * @return Cleaned query safe for Elasticsearch
     */
    public static String cleanQuery(String query) {
        if (!StringUtils.hasText(query)) {
            logger.debug("Empty or null query provided, returning empty string");
            return "";
        }

        try {
            String cleaned = query.trim();

            // Étape 1: Normaliser les guillemets typographiques et apostrophes
            cleaned = cleaned.replaceAll("[\"“”]", " ")  // guillemets doubles + guillemets typographiques
                    .replaceAll("[‘’']", " "); // apostrophes typographiques + apostrophe simple

            // Étape 2: Supprimer tous les caractères spéciaux d'Elasticsearch
            cleaned = ELASTICSEARCH_SPECIAL_CHARS.matcher(cleaned).replaceAll(" ");

            // Étape 3: Supprimer les autres caractères spéciaux (optionnel, plus restrictif)
            cleaned = QUERY_CLEANUP_PATTERN.matcher(cleaned).replaceAll(" ");

            // Étape 4: Normaliser les espaces multiples
            cleaned = cleaned.replaceAll("\\s+", " ").trim();

            // Étape 5: Convertir en minuscules pour la cohérence
            cleaned = cleaned.toLowerCase();

            logger.debug("Query cleaned: '{}' -> '{}'", query, cleaned);

            // Validation finale
            if (containsProblematicChars(cleaned)) {
                logger.warn("Query still contains problematic characters after cleaning: '{}'", cleaned);
                // En dernier recours, ne garder que les caractères alphanumériques et espaces
                cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
            }

            return cleaned;

        } catch (Exception e) {
            logger.error("Failed to clean query: '{}'", query, e);
            // En cas d'erreur, retourner une version ultra-sécurisée
            return query.replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase();
        }
    }

    /**
     * Vérifie si la chaîne contient des caractères problématiques pour Elasticsearch
     */
    private static boolean containsProblematicChars(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        // Vérifier la présence de caractères spéciaux Elasticsearch
        return text.contains("\"") || text.contains("*") || text.contains("+") ||
                text.contains("-") || text.contains("!") || text.contains("(") ||
                text.contains(")") || text.contains("{") || text.contains("}") ||
                text.contains("[") || text.contains("]") || text.contains("^") ||
                text.contains("~") || text.contains("?") || text.contains(":") ||
                text.contains("\\") || text.contains("/");
    }

    /**
     * Version encore plus stricte du nettoyage pour les cas critiques
     */
    public static String cleanQueryStrict(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }

        try {
            // Ne garder que les lettres, chiffres et espaces
            String cleaned = query.replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase();

            logger.debug("Query cleaned (strict): '{}' -> '{}'", query, cleaned);
            return cleaned;

        } catch (Exception e) {
            logger.error("Failed to clean query (strict): '{}'", query, e);
            return "";
        }
    }

    /**
     * Splits a query into individual terms.
     *
     * @param query The search query
     * @return Array of query terms
     */
    public static String[] splitQueryTerms(String query) {
        if (!StringUtils.hasText(query)) {
            logger.debug("Empty or null query provided, returning empty array");
            return new String[0];
        }

        try {
            String cleanedQuery = cleanQuery(query);
            if (!StringUtils.hasText(cleanedQuery)) {
                logger.debug("Query became empty after cleaning, returning empty array");
                return new String[0];
            }

            String[] terms = cleanedQuery.split("\\s+");
            logger.debug("Split query '{}' into {} terms", query, terms.length);
            return terms;

        } catch (Exception e) {
            logger.error("Failed to split query terms: '{}'", query, e);
            // En cas d'erreur, essayer de retourner la requête originale nettoyée
            String fallback = cleanQueryStrict(query);
            return StringUtils.hasText(fallback) ? new String[]{fallback} : new String[0];
        }
    }

    /**
     * Valide qu'une requête est sûre pour Elasticsearch
     */
    public static boolean isQuerySafe(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }

        // Vérifier qu'il n'y a pas de caractères problématiques
        return !containsProblematicChars(query) &&
                !query.trim().isEmpty() &&
                query.length() <= 1000; // Limite raisonnable
    }

    /**
     * Échappe les caractères spéciaux pour une utilisation dans Elasticsearch
     * (alternative au nettoyage complet)
     */
    public static String escapeElasticsearchSpecialChars(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }

        return query.replaceAll("([\"*+\\-!(){}\\[\\]^~?:\\\\/])", "\\\\$1");
    }
}