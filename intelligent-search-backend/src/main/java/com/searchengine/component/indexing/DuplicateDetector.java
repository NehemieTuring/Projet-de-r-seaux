package com.searchengine.component.indexing;

import com.searchengine.model.entity.SearchDocument;
import com.searchengine.repository.elasticsearch.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Component for detecting duplicate documents based on content hash.
 */
@Component
public class DuplicateDetector {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetector.class);
    private final DocumentRepository documentRepository;

    public DuplicateDetector(@Lazy DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Checks if a document is a duplicate based on content hash.
     *
     * @param searchDocument The document to check
     * @return true if duplicate, false otherwise
     */
    public boolean isDuplicate(SearchDocument searchDocument) {
        logger.debug("Checking for duplicate document: {}", searchDocument.getUrl());
        try {
            if (searchDocument.getContent() == null || searchDocument.getContent().isEmpty()) {
                logger.warn("Empty content for document: {}", searchDocument.getUrl());
                return false;
            }

            String contentHash = calculateContentHash(searchDocument.getContent());
            searchDocument.setContentHash(contentHash); // utile pour l'indexation ensuite

            boolean exists = documentRepository.existsByContentHash(contentHash);

            logger.debug("Duplicate check for {}: exists={}, contentHash={}", searchDocument.getUrl(), exists, contentHash);
            return exists;
        } catch (Exception e) {
            logger.error("Failed to check for duplicate document: {}", searchDocument.getUrl(), e);
            return false;
        }
    }

    private String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to calculate content hash", e);
            return "";
        }
    }
}
