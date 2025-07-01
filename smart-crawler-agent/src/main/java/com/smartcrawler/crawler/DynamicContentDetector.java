package com.smartcrawler.crawler;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.utils.HttpUtils;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Détecteur de contenu dynamique (CSR) dans les pages web.
 * Analyse le DOM pour identifier les frameworks JS et les balises indicatrices.
 */
public class DynamicContentDetector {

    private final Logger logger;
    private static final Set<String> DYNAMIC_INDICATORS = new HashSet<>(Arrays.asList(
            "div[id=root]", "div[id=app]", "div[data-reactroot]", "div[ng-app]", "div[ng-version]"
    ));

    public DynamicContentDetector() {
        this.logger = LoggerFactory.getLogger(DynamicContentDetector.class.getName());
    }

    /**
     * Détermine si une page web contient du contenu dynamique.
     *
     * @param url    URL de la page
     * @param config Configuration du crawl
     * @return true si la page est dynamique, false sinon
     */
    public boolean isDynamicContent(String url, CrawlConfig config) {
        if (!config.isDetectDynamicContent()) {
            logger.debug("Détection de contenu dynamique désactivée pour: " + url, null);
            return false;
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(HttpUtils.getUserAgent())
                    .timeout(ConfigManager.getInstance().getIntProperty("crawler.request.timeout", 10000))
                    .get();

            for (String selector : DYNAMIC_INDICATORS) {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    logger.info("Contenu dynamique détecté (sélecteur: " + selector + ") pour: " + url, null);
                    return true;
                }
            }

            // Vérification des scripts JavaScript
            boolean hasJsFrameworks = doc.select("script[src]").stream()
                    .anyMatch(e -> e.attr("src").toLowerCase().matches(".*(react|vue|angular).*"));
            if (hasJsFrameworks) {
                logger.info("Contenu dynamique détecté (framework JS) pour: " + url, null);
                return true;
            }

            logger.debug("Aucun contenu dynamique détecté pour: " + url, null);
            return false;
        } catch (IOException e) {
            logger.error("Erreur lors de la détection de contenu dynamique pour: " + url, e);
            return false;
        }
    }
}