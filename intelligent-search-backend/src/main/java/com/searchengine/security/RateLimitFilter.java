package com.searchengine.security;

import com.searchengine.exception.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

/**
 * Filter for rate limiting requests by IP address.
 */
@Component
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final RedisTemplate<String, Object> redisTemplate;
    private final int maxRequestsPerSecond;

    public RateLimitFilter(RedisTemplate<String, Object> redisTemplate,
                           @Value("${security.rate.limit.per.second:5}") int maxRequestsPerSecond) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Applies rate limiting by IP address using Redis.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain
     * @throws ServletException if filter processing fails
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String key = "rate_limit:" + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            // Set expiration only on first increment
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(1));
            }

            if (currentCount > maxRequestsPerSecond) {
                logger.warn("Rate limit exceeded for IP: {}, count: {}, limit: {}",
                        clientIp, currentCount, maxRequestsPerSecond);

                response.setStatus(HTTP_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                String jsonResponse = String.format(
                        "{\"errorCode\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests. Limit: %d requests per second\"}",
                        maxRequestsPerSecond
                );
                response.getWriter().write(jsonResponse);
                return;
            }

            logger.debug("Rate limit check passed for IP: {}, count: {}/{}",
                    clientIp, currentCount, maxRequestsPerSecond);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Rate limit filter error for IP: {}", clientIp, e);
            // Continue with the request instead of throwing exception to avoid breaking the filter chain
            logger.warn("Continuing request processing due to rate limit error");
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract the real client IP address considering proxy headers.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Get the first IP in case of multiple IPs
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Skip rate limiting for certain endpoints if needed.
     * Override this method to customize which requests should be rate limited.
     *
     * @param request HTTP request
     * @return true if request should be filtered, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip rate limiting for health check endpoints
        return path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/favicon.ico");
    }
}