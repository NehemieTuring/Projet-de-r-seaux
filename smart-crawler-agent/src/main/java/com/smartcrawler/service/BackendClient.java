package com.smartcrawler.service;

import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.HttpUtils;
import com.smartcrawler.model.ClientCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class BackendClient {
    private static final Logger logger = LoggerFactory.getLogger(BackendClient.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final okhttp3.OkHttpClient client;
    private final int maxRetries = 1;
    private final long retryDelayMs = 1000;
    private final boolean circuitBreakerEnabled = true;
    private boolean circuitOpen = false;
    private long lastFailureTime = 0;

    public BackendClient() {
        this.client = new okhttp3.OkHttpClient();
    }

    public String postAuth(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        String json = objectMapper.writeValueAsString(credentials);
        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return executeWithRetry(request, "Authentication");
    }

    public String postRegister(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        String json = objectMapper.writeValueAsString(credentials);
        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return executeWithRetry(request, "Registration");
    }

    public String resetCredentials(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        String json = objectMapper.writeValueAsString(credentials);
        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return executeWithRetry(request, "Reset credentials");
    }

    public String updateAccount(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        String json = objectMapper.writeValueAsString(credentials);
        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        return executeWithRetry(request, "Update account");
    }

    public void deleteAccount(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        String json = objectMapper.writeValueAsString(credentials);
        RequestBody body = RequestBody.create(json, HttpUtils.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();

        executeWithRetry(request, "Delete account");
    }

    public String validateToken(String url, ClientCredentials credentials, String apiKeyHeaderName) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(apiKeyHeaderName, credentials.getApiKey())
                .build();

        return executeWithRetry(request, "Token validation");
    }

    private String executeWithRetry(Request request, String operation) throws IOException {
        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    logger.info(operation + " request successful", null);
                    return response.body().string();
                } else {
                    logger.warn(operation + " failed, code: " + response.code() + ", attempt: " + attempt, null);
                }
            } catch (IOException e) {
                logger.error("Network error during " + operation.toLowerCase() + ", attempt: " + attempt, e);
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry", ie);
                    throw new IOException(operation + " interrupted", ie);
                }
            }
        }

        if (circuitBreakerEnabled) {
            circuitOpen = true;
            lastFailureTime = System.currentTimeMillis();
            logger.error("Circuit breaker opened after failed " + operation.toLowerCase() + " retries", null);
        }
        throw new IOException(operation + " failed after " + maxRetries + " attempts");
    }
}