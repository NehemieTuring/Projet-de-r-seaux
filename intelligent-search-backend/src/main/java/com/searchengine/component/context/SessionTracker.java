package com.searchengine.component.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Component for tracking user sessions using Redis.
 */
@Component
public class SessionTracker {

    private static final Logger logger = LoggerFactory.getLogger(SessionTracker.class);
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.session.ttl-minutes:30}")
    private long sessionTtlMinutes;

    public SessionTracker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates or retrieves a session ID for a user.
     *
     * @param clientIp The client IP address
     * @return Session ID
     */
    public String getOrCreateSessionId(String clientIp) {
        logger.debug("Getting or creating session for IP: {}", clientIp);
        try {
            String sessionKey = "session:" + clientIp;
            String sessionId = (String) redisTemplate.opsForValue().get(sessionKey);

            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                redisTemplate.opsForValue().set(sessionKey, sessionId, sessionTtlMinutes, TimeUnit.MINUTES);
                logger.debug("Created new session ID: {} for IP: {}", sessionId, clientIp);
            } else {
                // Refresh TTL
                redisTemplate.expire(sessionKey, sessionTtlMinutes, TimeUnit.MINUTES);
                logger.debug("Retrieved existing session ID: {} for IP: {}", sessionId, clientIp);
            }

            return sessionId;
        } catch (Exception e) {
            logger.error("Failed to manage session for IP: {}", clientIp, e);
            return UUID.randomUUID().toString(); // Fallback to new session ID
        }
    }
}