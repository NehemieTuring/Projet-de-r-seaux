package com.searchengine.service;

import com.searchengine.utils.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing cache operations using Redis.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.ttl-seconds:3600}")
    private long defaultTtlSeconds;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Caches a search result for a given query.
     *
     * @param query The search query
     * @param result The search result
     */
    public void cacheSearchResult(String query, Object result) {
        logger.debug("Caching search result for query: {}", query);
        try {
            String cacheKey = "search:" + query;
            CacheUtils.cacheObject(redisTemplate, cacheKey, result, defaultTtlSeconds);
            logger.debug("Search result cached for query: {}", query);
        } catch (Exception e) {
            logger.error("Failed to cache search result for query: {}", query, e);
        }
    }

    /**
     * Retrieves a cached search result for a given query.
     *
     * @param query The search query
     * @return Cached result or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedSearchResult(String query) {
        logger.debug("Retrieving cached search result for query: {}", query);
        try {
            String cacheKey = "search:" + query;
            T result = (T) CacheUtils.getCachedObject(redisTemplate, cacheKey);
            logger.debug("Cache {} for query: {}", result != null ? "hit" : "miss", query);
            return result;
        } catch (Exception e) {
            logger.error("Failed to retrieve cached search result for query: {}", query, e);
            return null;
        }
    }
}