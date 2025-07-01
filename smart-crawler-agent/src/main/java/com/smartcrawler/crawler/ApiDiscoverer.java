package com.smartcrawler.crawler;

import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.model.CrawlResult;
import com.smartcrawler.service.SecurityManager;
import com.smartcrawler.utils.HttpUtils;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.ValidationUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Découvreur d'endpoints API REST/GraphQL.
 * Explore les URLs potentielles et teste leur accessibilité.
 */
public class ApiDiscoverer {

    private final Logger logger;
    private final OkHttpClient client;
    private final SecurityManager securityManager;
    private static final Pattern API_PATH_PATTERN = Pattern.compile(".*(api|graphql|v[0-9]+).*");

    public ApiDiscoverer() {
        this.logger = LoggerFactory.getLogger(ApiDiscoverer.class.getName());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .writeTimeout(10000, TimeUnit.MILLISECONDS)
                .build();
        this.securityManager = new SecurityManager();
    }

    /**
     * Découvre les endpoints API à partir d'une URL de base.
     *
     * @param config Configuration du crawl
     * @param url    URL de base
     * @param depth  Profondeur actuelle de la découverte
     * @return Liste des endpoints API détectés
     */
    public List<CrawlResult.ApiEndpoint> discoverApis(CrawlConfig config, String url, int depth) {
        List<CrawlResult.ApiEndpoint> endpoints = new ArrayList<>();
        if (!config.isDetectApis() || depth > config.getApiScanDepth()) {
            logger.debug("Détection des APIs désactivée ou profondeur maximale atteinte pour: " + url, null);
            return endpoints;
        }

        if (!ValidationUtils.isValidUrl(url) || !securityManager.isUrlAllowed(url)) {
            logger.warn("URL non autorisée ou invalide pour la découverte d'API: " + url, null);
            return endpoints;
        }

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head() // Utiliser HEAD pour minimiser la charge
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String contentType = response.header("Content-Type", "");
                    if (contentType.contains("application/json") || contentType.contains("graphql")) {
                        endpoints.add(new CrawlResult.ApiEndpoint(url, "GET"));
                        logger.info("Endpoint API détecté: " + url, null);
                    }
                }
            } catch (IOException e) {
                logger.error("Erreur lors de la requête HEAD pour: " + url, e);
            }

            // TODO: Ajouter une exploration recursive des sous-chemins potentiels (/api/v1, /graphql, etc.)
        } catch (Exception e) {
            logger.error("Erreur lors de la découverte d'API pour: " + url, e);
        }

        return endpoints;
    }
}