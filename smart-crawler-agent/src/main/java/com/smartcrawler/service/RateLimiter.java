package com.smartcrawler.service;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Limiteur de débit pour contrôler les requêtes par domaine.
 * Utilise un algorithme de token bucket par domaine.
 */
public class RateLimiter {

    private final Logger logger;
    private final ConfigManager configManager;
    private final Map<String, TokenBucket> buckets;
    private final double requestsPerSecond;

    public RateLimiter() {
        this.logger = LoggerFactory.getLogger(RateLimiter.class.getName());
        this.configManager = ConfigManager.getInstance();
        this.requestsPerSecond = configManager.getIntProperty("security.rate.limit.per.second", 5);
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * Vérifie si une requête est autorisée pour une URL donnée.
     *
     * @param url URL de la requête
     * @return true si la requête est autorisée, false sinon
     */
    public boolean allowRequest(String url) {
        try {
            String domain = new URL(url).getHost();
            TokenBucket bucket = buckets.computeIfAbsent(domain, k -> new TokenBucket(requestsPerSecond));
            boolean allowed = bucket.allowRequest();
            if (!allowed) {
                logger.debug("Requête bloquée par limiteur de débit pour le domaine: " + domain, null);
            }
            return allowed;
        } catch (Exception e) {
            logger.error("Erreur lors de la validation du débit pour l'URL: " + url, e);
            return false;
        }
    }

    private static class TokenBucket {
        private final double rate;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(double rate) {
            this.rate = rate;
            this.tokens = rate;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean allowRequest() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            tokens = Math.min(rate, tokens + elapsedSeconds * rate);
            lastRefillTimestamp = now;
        }
    }
}