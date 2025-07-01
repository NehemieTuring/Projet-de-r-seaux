package com.smartcrawler.crawler;

import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

/**
 * Extracteur de contenu multi-format.
 * Fournit des méthodes pour extraire le texte et les métadonnées de différents types de contenu.
 */
public class ContentExtractor {

    private final Logger logger;

    public ContentExtractor() {
        this.logger = LoggerFactory.getLogger(ContentExtractor.class.getName());
    }

    /**
     * Extrait le contenu textuel d'un document HTML.
     *
     * @param document Document Jsoup à analyser
     * @return Contenu textuel extrait
     */
    public String extractTextContent(Document document) {
        if (document == null) {
            logger.error("Document null lors de l'extraction de contenu", null);
            return "";
        }

        try {
            // Supprime les scripts, styles, et commentaires
            document.select("script, style, noscript").remove();
            String text = document.text();
            text = StringUtils.normalizeSpace(text);
            if (text.length() > 10000) { // Limite arbitraire pour éviter les contenus trop volumineux
                text = text.substring(0, 10000);
                logger.warn("Contenu tronqué pour : " + document.location(), null);
            }
            return text;
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction de contenu pour : " + document.location(), e);
            return "";
        }
    }

    /**
     * Extrait le contenu textuel d'un document à partir de son URL (ex. PDF, .docx, .txt).
     *
     * @param url URL du document
     * @return Contenu textuel extrait, ou chaîne vide si non supporté
     */
    public String extractTextContentFromUrl(String url) {
        // TODO: Implémenter l'extraction de texte pour les PDF (ex. via Apache PDFBox)
        logger.warn("Extraction de contenu pour URL non implémentée : " + url, null);
        return "";
    }
}