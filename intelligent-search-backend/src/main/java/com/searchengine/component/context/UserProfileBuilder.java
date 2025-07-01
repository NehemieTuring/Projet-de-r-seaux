package com.searchengine.component.context;

import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component for building user profiles based on session data and user agent.
 */
@Component
public class UserProfileBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileBuilder.class);

    /**
     * Builds a user profile for a given session.
     *
     * @param sessionId The session ID
     * @param userAgent The user agent string
     * @return UserProfile containing profile data
     */
    public SearchContext.UserProfile buildProfile(String sessionId, String userAgent) {
        logger.debug("Building user profile for session: {}", sessionId);
        try {
            SearchContext.UserProfile userProfile = new SearchContext.UserProfile();
            userProfile.setDeviceType(parseDeviceType(userAgent));
            userProfile.setPreferredLanguage(parsePreferredLanguage(userAgent));

            // Placeholder: Implement actual profile building logic (e.g., from session history)
            logger.debug("User profile built for session: {} - {}", sessionId, userProfile);
            return userProfile;
        } catch (Exception e) {
            logger.error("Failed to build user profile for session: {}", sessionId, e);
            return new SearchContext.UserProfile(); // Return default empty profile
        }
    }

    /**
     * Parses the device type from the user agent string.
     *
     * @param userAgent The user agent string
     * @return Device type (e.g., Mobile, Desktop)
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        return userAgent.toLowerCase().contains("mobile") ? "Mobile" : "Desktop";
    }

    /**
     * Parses the preferred language from the user agent string.
     *
     * @param userAgent The user agent string
     * @return Preferred language (e.g., en, fr)
     */
    private String parsePreferredLanguage(String userAgent) {
        // Placeholder: Implement actual language detection (e.g., from Accept-Language header)
        return "en";
    }
}