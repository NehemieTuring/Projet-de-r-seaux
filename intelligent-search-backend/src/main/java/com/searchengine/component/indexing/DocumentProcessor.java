package com.searchengine.component.indexing;

import com.searchengine.model.entity.SearchDocument;
import com.searchengine.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component for processing documents before indexing.
 */
@Component
public class DocumentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);
    private final ContentAnalyzer contentAnalyzer;

    public DocumentProcessor(ContentAnalyzer contentAnalyzer) {
        this.contentAnalyzer = contentAnalyzer;
    }

    /**
     * Processes a document by validating and analyzing its content.
     *
     * @param searchDocument The document to process
     */
    public void processDocument(SearchDocument searchDocument) {
        logger.debug("Processing document with URL: {}", searchDocument.getUrl());
        try {
            // Validate URL
            if (!ValidationUtils.isValidUrl(searchDocument.getUrl())) {
                throw new IllegalArgumentException("Invalid URL: " + searchDocument.getUrl());
            }

            // Validate content type
            if (!ValidationUtils.isValidContentType(searchDocument.getDocumentType())) {
                throw new IllegalArgumentException("Invalid content type: " + searchDocument.getDocumentType());
            }

            if (searchDocument.getContent() == null || searchDocument.getContent().isBlank()) {
                logger.warn("Document content is empty for URL: {}", searchDocument.getUrl());
                return;
            }

            // Analyze content
            contentAnalyzer.analyzeContent(searchDocument);

            // Clean content
            if (searchDocument.getContent() != null) {
                searchDocument.setContent(cleanContent(searchDocument.getContent()));
            }

            // Set language from metadata
            if (searchDocument.getMetadata() != null && searchDocument.getMetadata().containsKey("language")) {
                searchDocument.setLanguage(searchDocument.getMetadata().get("language"));
            } else {
                logger.warn("No language found in metadata for document: {}", searchDocument.getUrl());
            }

            logger.debug("Document processed successfully: {}", searchDocument.getUrl());
        } catch (Exception e) {
            logger.error("Failed to process document: {}", searchDocument.getUrl(), e);
            throw new RuntimeException("Document processing failed: " + searchDocument.getUrl(), e);
        }
    }

    /**
     * Cleans content by removing HTML tags and normalizing text.
     *
     * @param content Raw content
     * @return Cleaned content
     */
    private String cleanContent(String content) {
        // Simple HTML tag removal (can be enhanced with Jsoup)
        return content.replaceAll("<[^>]+>", "").trim();
    }
}