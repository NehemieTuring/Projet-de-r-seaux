package com.searchengine.utils;

import com.searchengine.model.entity.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for geolocation-related operations.
 */
public class GeoUtils {

    private static final Logger logger = LoggerFactory.getLogger(GeoUtils.class);

    /**
     * Calculates the distance between two geolocation points using the Haversine formula.
     *
     * @param geo1 First geolocation
     * @param geo2 Second geolocation
     * @return Distance in kilometers
     */
    public static double calculateDistance(SearchContext.GeoLocation geo1, SearchContext.GeoLocation geo2) {
        logger.debug("Calculating distance between {} and {}", geo1, geo2);
        try {
            if (geo1 == null || geo2 == null || geo1.getLatitude() == 0.0 || geo2.getLatitude() == 0.0) {
                logger.warn("Invalid geolocation data for distance calculation");
                return 0.0;
            }

            double lat1 = Math.toRadians(geo1.getLatitude());
            double lon1 = Math.toRadians(geo1.getLongitude());
            double lat2 = Math.toRadians(geo2.getLatitude());
            double lon2 = Math.toRadians(geo2.getLongitude());

            double dlon = lon2 - lon1;
            double dlat = lat2 - lat1;
            double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
            double c = 2 * Math.asin(Math.sqrt(a));
            double r = 6371; // Earth's radius in kilometers

            double distance = c * r;
            logger.debug("Calculated distance: {} km", distance);
            return distance;
        } catch (Exception e) {
            logger.error("Failed to calculate distance between {} and {}", geo1, geo2, e);
            return 0.0;
        }
    }
}