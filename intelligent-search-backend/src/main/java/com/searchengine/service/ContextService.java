package com.searchengine.service;

import com.searchengine.component.context.ContextAnalyzer;
import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing search context, including user profile and geolocation.
 */
@Service
public class ContextService {

    private static final Logger logger = LoggerFactory.getLogger(ContextService.class);
    private final ContextAnalyzer contextAnalyzer;

    @Autowired
    public ContextService(ContextAnalyzer contextAnalyzer) {
        this.contextAnalyzer = contextAnalyzer;
    }

    /**
     * Builds and analyzes the search context for a given session and request data.
     *
     * @param sessionId The session ID
     * @param ipAddress The client IP address
     * @param userAgent The user agent string
     * @return SearchContext containing user profile and geolocation
     */
    public SearchContext buildContext(String sessionId, String ipAddress, String userAgent) {
        logger.info("Building search context for session: {}", sessionId);
        try {
            SearchContext context = contextAnalyzer.analyzeContext(sessionId, ipAddress, userAgent);
            logger.debug("Search context built successfully for session: {}", sessionId);
            return context;
        } catch (Exception e) {
            logger.error("Failed to build search context for session: {}", sessionId, e);
            throw new RuntimeException("Failed to build search context for session: " + sessionId, e);
        }
    }
}