package com.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for cache operations using Redis.
 */
public class CacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    /**
     * Caches an object in Redis with a specified TTL.
     *
     * @param redisTemplate Redis template
     * @param key Cache key
     * @param value Object to cache
     * @param ttl Time to live in seconds
     * @return true if caching was successful, false otherwise
     */
    public static <T> boolean cacheObject(RedisTemplate<String, T> redisTemplate, String key, T value, long ttl) {
        logger.debug("Caching object with key: {}", key);
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            logger.debug("Object cached successfully with key: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to cache object with key: {}", key, e);
            return false;
        }
    }

    /**
     * Retrieves an object from Redis cache.
     *
     * @param redisTemplate Redis template
     * @param key Cache key
     * @return Cached object or null if not found
     */
    public static <T> T getCachedObject(RedisTemplate<String, T> redisTemplate, String key) {
        logger.debug("Retrieving cached object with key: {}", key);
        try {
            T value = redisTemplate.opsForValue().get(key);
            logger.debug("Cache hit for key: {}, value: {}", key, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            logger.error("Failed to retrieve cached object with key: {}", key, e);
            return null;
        }
    }
}