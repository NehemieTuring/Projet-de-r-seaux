package com.searchengine.component.indexing;

import com.searchengine.model.entity.SearchDocument;
import com.searchengine.model.enums.DocumentType;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class ContentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ContentAnalyzer.class);
    private final AutoDetectParser parser;

    public ContentAnalyzer() {
        this.parser = new AutoDetectParser();
    }

    public void analyzeContent(SearchDocument searchDocument) {
        logger.debug("Analyzing content for document: {}", searchDocument.getUrl());
        try {
            BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024); // 10MB limit
            Metadata metadata = new Metadata();
            String content = searchDocument.getContent() != null ? searchDocument.getContent() : "";
            if (content.isBlank()) {
                logger.warn("Empty content for document: {}", searchDocument.getUrl());
                return;
            }
            parser.parse(new ByteArrayInputStream(content.getBytes()), handler, metadata);

            if (searchDocument.getTitle() == null || searchDocument.getTitle().isBlank()) {
                String title = metadata.get(TikaCoreProperties.TITLE);
                if (title != null) {
                    searchDocument.setTitle(title);
                }
            }

            if (searchDocument.getDescription() == null || searchDocument.getDescription().isBlank()) {
                String description = metadata.get(TikaCoreProperties.DESCRIPTION);
                if (description != null) {
                    searchDocument.setDescription(description);
                }
            }

            // Utiliser DocumentType à la place de ContentType
            if (searchDocument.getDocumentType() == null) {
                String contentType = metadata.get(Metadata.CONTENT_TYPE);
                if (contentType != null) {
                    searchDocument.setDocumentType(mapToDocumentType(contentType));
                }
            }

            logger.debug("Content analysis completed for document: {}", searchDocument.getUrl());
        } catch (Exception e) {
            logger.error("Failed to analyze content for document: {}", searchDocument.getUrl(), e);
            throw new RuntimeException("Content analysis failed: " + searchDocument.getUrl(), e);
        }
    }

    /**
     * Mappe le content-type Tika vers le DocumentType.
     */
    private DocumentType mapToDocumentType(String tikaContentType) {
        if (tikaContentType == null) {
            return DocumentType.UNKNOWN;
        }

        return switch (tikaContentType.toLowerCase()) {
            case "text/html" -> DocumentType.WEB_PAGE;
            case "application/pdf" -> DocumentType.PDF;
            case "image/jpeg", "image/png" -> DocumentType.IMAGE;
            case "video/mp4" -> DocumentType.VIDEO;
            default -> DocumentType.UNKNOWN;
        };
    }
}
