package com.searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity representing the search context for a user.
 */
@Data
@Entity
@Table(name = "search_contexts")
public class SearchContext {

    @Id
    private String sessionId;

    @Embedded
    private GeoLocation geoLocation;

    @Embedded
    private UserProfile userProfile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Inner class for geolocation data.
     */
    @Data
    @Embeddable
    public static class GeoLocation {
        private String country;
        private String city;
        private Double latitude;
        private Double longitude;
    }

    /**
     * Inner class for user profile data.
     */
    @Data
    @Embeddable
    public static class UserProfile {
        private String deviceType;
        private String preferredLanguage;
    }
}