package com.smartcrawler.utils;

import com.smartcrawler.core.ConfigManager;
import okhttp3.MediaType;

import java.io.IOException;

/**
 * Utilitaires pour les opérations HTTP.
 */
public class HttpUtils {

    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class.getName());

    /**
     * Construit un user-agent à partir de la configuration.
     *
     * @return User-agent configuré
     */
    public static String getUserAgent() {
        ConfigManager config = ConfigManager.getInstance();
        boolean rotateUserAgent = config.getBooleanProperty("crawler.user.agent.rotation", true);
        // TODO: Implémenter la rotation des user-agents
        return "SmartCrawler/1.0";
    }

    /**
     * Vérifie si une redirection est autorisée selon la configuration.
     *
     * @param redirectCount Nombre actuel de redirections
     * @return true si la redirection est autorisée, false sinon
     */
    public static boolean isRedirectAllowed(int redirectCount) {
        ConfigManager config = ConfigManager.getInstance();
        int maxRedirects = config.getIntProperty("security.max.redirects", 5);
        if (redirectCount > maxRedirects) {
            logger.warn("Nombre maximum de redirections dépassé: " + redirectCount, null);
            return false;
        }
        return true;
    }

    public static String readText(String url) throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return "";
        }
    }

}