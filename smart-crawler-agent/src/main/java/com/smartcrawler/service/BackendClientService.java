package com.smartcrawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.IndexPayload;
import com.smartcrawler.utils.HttpUtils;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Client HTTP pour communiquer avec le backend via l'API REST.
 * Gère l'envoi des payloads avec retry et circuit breaker.
 */
public class BackendClientService {

    private final Logger logger;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String backendUrl;
    private final int maxRetries;
    private final long retryDelayMs;
    private final boolean circuitBreakerEnabled;
    private boolean circuitOpen = false;
    private long lastFailureTime = 0;
    private final long circuitResetTimeoutMs = 60000; // 1 minute

    public BackendClientService() {
        this.logger = LoggerFactory.getLogger(BackendClientService.class.getName());
        ConfigManager config = ConfigManager.getInstance();
        this.backendUrl = config.getProperty("backend.url", "http://localhost:8080/api/index");
        int timeout = config.getIntProperty("backend.timeout", 30000);
        this.maxRetries = config.getIntProperty("backend.retry.attempts", 3);
        this.retryDelayMs = 1000; // Backoff initial
        this.circuitBreakerEnabled = config.getBooleanProperty("backend.circuit.breaker.enabled", true);
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Envoie un payload au backend.
     *
     * @param payload Payload à envoyer
     * @return true si l'envoi est réussi, false sinon
     */
    public boolean sendPayload(IndexPayload payload) {
        if (payload == null) {
            logger.error("Payload null", null);
            return false;
        }

        if (circuitBreakerEnabled && circuitOpen) {
            if (System.currentTimeMillis() - lastFailureTime > circuitResetTimeoutMs) {
                circuitOpen = false;
                logger.info("Circuit breaker réinitialisé", null);
            } else {
                logger.warn("Circuit breaker ouvert, envoi annulé", null);
                return false;
            }
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            logger.error("Erreur de sérialisation du payload", e);
            return false;
        }

        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(backendUrl)
                .post(body)
                .build();

        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Payload envoyé avec succès, batchId: " + payload.getBatchId(), null);
                    return true;
                } else {
                    logger.warn("Échec de l'envoi, code HTTP: " + response.code() + ", tentative: " + attempt, null);
                }
            } catch (IOException e) {
                logger.error("Erreur réseau lors de l'envoi, tentative: " + attempt, e);
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interruption lors du retry", ie);
                    return false;
                }
            }
        }

        if (circuitBreakerEnabled) {
            circuitOpen = true;
            lastFailureTime = System.currentTimeMillis();
            logger.error("Circuit breaker ouvert après échec des retries", null);
        }
        return false;
    }
}