package com.searchengine.component.indexing;

import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.langdetect.opennlp.OpenNLPDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Composant pour détecter la langue du contenu d'un document en utilisant Apache Tika avec OpenNLP.
 */
@Component
public class TikaLanguageDetector {

    private static final Logger logger = LoggerFactory.getLogger(TikaLanguageDetector.class);
    private final LanguageDetector tikaLanguageDetector;

    public TikaLanguageDetector() {
        this.tikaLanguageDetector = new OpenNLPDetector();
        try {
            // Chargement des modèles de langue
            this.tikaLanguageDetector.loadModels();
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des modèles de langue OpenNLP", e);
            throw new RuntimeException("Impossible d'initialiser le détecteur de langue", e);
        }
    }

    /**
     * Détecte la langue du contenu fourni.
     *
     * @param content Contenu du document
     * @return Code de la langue détectée (par exemple, "en", "fr") ou "unknown" si la détection échoue
     */
    public String detectLanguage(String content) {
        logger.debug("Détection de la langue pour l'extrait de contenu : {}",
                content != null ? content.substring(0, Math.min(50, content.length())) : "null");

        try {
            if (content == null || content.trim().isEmpty()) {
                logger.warn("Contenu vide fourni pour la détection de langue");
                return "unknown";
            }

            LanguageResult result = tikaLanguageDetector.detect(content);
            String language = result.isReasonablyCertain() ? result.getLanguage() : "unknown";

            logger.debug("Langue détectée : {} (confiance : {})",
                    language, result.getRawScore());

            return language;
        } catch (Exception e) {
            logger.error("Échec de la détection de la langue pour le contenu", e);
            return "unknown";
        }
    }

    /**
     * Détecte la langue avec plus de détails sur la confiance.
     *
     * @param content Contenu du document
     * @return Résultat complet de la détection
     */
    public LanguageResult detectLanguageWithDetails(String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            return tikaLanguageDetector.detect(content);
        } catch (Exception e) {
            logger.error("Échec de la détection détaillée de la langue", e);
            return null;
        }
    }
}