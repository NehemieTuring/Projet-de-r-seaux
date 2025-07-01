package com.searchengine.component.context;

import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Component for analyzing search context, combining geolocation and user profile.
 */
@Component
public class ContextAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ContextAnalyzer.class);
    private final GeoLocationDetector geoLocationDetector;
    private final UserProfileBuilder userProfileBuilder;

    @Autowired
    public ContextAnalyzer(GeoLocationDetector geoLocationDetector, UserProfileBuilder userProfileBuilder) {
        this.geoLocationDetector = geoLocationDetector;
        this.userProfileBuilder = userProfileBuilder;
    }

    /**
     * Analyzes the context for a search request.
     *
     * @param sessionId The session ID
     * @param ipAddress The client IP address
     * @param userAgent The user agent string
     * @return SearchContext with analyzed data
     */
    public SearchContext analyzeContext(String sessionId, String ipAddress, String userAgent) {
        logger.debug("Analyzing context for session: {}", sessionId);
        try {
            SearchContext context = new SearchContext();
            context.setSessionId(sessionId);

            // Detect geolocation
            SearchContext.GeoLocation geoLocation = geoLocationDetector.detectLocation(ipAddress);
            context.setGeoLocation(geoLocation);

            // Build user profile
            SearchContext.UserProfile userProfile = userProfileBuilder.buildProfile(sessionId, userAgent);
            context.setUserProfile(userProfile);

            logger.debug("Context analysis completed for session: {}", sessionId);
            return context;
        } catch (Exception e) {
            logger.error("Failed to analyze context for session: {}", sessionId, e);
            throw new RuntimeException("Context analysis failed for session: " + sessionId, e);
        }
    }
}