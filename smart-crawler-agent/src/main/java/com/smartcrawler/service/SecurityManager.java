package com.smartcrawler.service;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.utils.HttpUtils;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.ValidationUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Gestionnaire de sécurité pour respecter les règles de crawl (robots.txt, validation d'URLs, redirections).
 */
public class SecurityManager {

    private final Logger logger;
    private final ConfigManager configManager;
    private final boolean respectRobotsTxt;
    private final int maxRedirects;

    // robots.txt : disallow par domaine
    private final Map<String, Set<String>> domainDisallowedPaths;

    public SecurityManager() {
        this.logger = LoggerFactory.getLogger(SecurityManager.class.getName());
        this.configManager = ConfigManager.getInstance();
        this.respectRobotsTxt = configManager.getBooleanProperty("security.respect.robots.txt", true);
        this.maxRedirects = configManager.getIntProperty("security.max.redirects", 5);
        this.domainDisallowedPaths = new HashMap<>();
    }

    /**
     * Vérifie si une URL est autorisée à être crawlée selon robots.txt et autres règles.
     *
     * @param url URL à vérifier
     * @return true si l'URL est autorisée, false sinon
     */
    public boolean isUrlAllowed(String url) {
        if (!ValidationUtils.isValidUrl(url)) {
            logger.warn("URL invalide: " + url, null);
            return false;
        }

        if (!respectRobotsTxt) return true;

        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            String path = parsedUrl.getPath();

            // Charger robots.txt si non encore chargé pour ce domaine
            if (!domainDisallowedPaths.containsKey(domain)) {
                loadRobotsTxt(domain, parsedUrl.getProtocol());
            }

            Set<String> disallowedPaths = domainDisallowedPaths.getOrDefault(domain, Set.of());

            // Vérifie si l'URL commence par un chemin interdit
            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    logger.debug("URL bloquée par robots.txt: " + url, null);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de robots.txt pour: " + url, e);
            return true; // Par défaut, on autorise
        }
    }

    /**
     * Charge et analyse le fichier robots.txt d'un domaine donné.
     *
     * @param domain   Domaine cible
     * @param protocol Protocole (http ou https)
     */
    private void loadRobotsTxt(String domain, String protocol) {
        String robotsUrl = protocol + "://" + domain + "/robots.txt";
        Set<String> disallowedPaths = new HashSet<>();

        try {
            String content = HttpUtils.readText(robotsUrl); // doit exister ou être implémenté
            String[] lines = content.split("\n");

            boolean appliesToUs = false;

            for (String line : lines) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.toLowerCase().startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim();
                    appliesToUs = agent.equals("*");
                } else if (appliesToUs && line.toLowerCase().startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    if (!path.isEmpty()) {
                        disallowedPaths.add(path);
                    }
                } else if (line.toLowerCase().startsWith("user-agent:")) {
                    // Nouvelle section, ne nous concerne pas
                    appliesToUs = false;
                }
            }

            domainDisallowedPaths.put(domain, disallowedPaths);
            logger.debug("robots.txt chargé pour le domaine: " + domain, null);
        } catch (IOException e) {
            logger.warn("Impossible de charger robots.txt pour: " + robotsUrl, e);
        }
    }

    /**
     * Vérifie si une redirection est autorisée selon la configuration.
     *
     * @param redirectCount Nombre actuel de redirections suivies
     * @return true si le nombre reste acceptable
     */
    public boolean isRedirectAllowed(int redirectCount) {
        return redirectCount < maxRedirects;
    }
}
