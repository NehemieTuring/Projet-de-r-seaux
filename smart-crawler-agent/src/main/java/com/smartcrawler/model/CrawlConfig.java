package com.smartcrawler.model;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.enums.DocumentType;
import com.smartcrawler.model.enums.CrawlProfile;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration typée pour une opération de crawl.
 */
@Getter
public class CrawlConfig {

    private final CrawlProfile profile;
    private final int maxDepth;
    private final int maxPagesPerDomain;
    private final Set<DocumentType> documentTypes;
    private final boolean followExternalLinks;
    private final int maxExternalDepth;
    private final boolean detectApis;
    private final boolean detectDynamicContent;
    private final int apiScanDepth;

    public CrawlConfig(CrawlProfile profile) {
        this.profile = profile != null ? profile : CrawlProfile.STANDARD;
        ConfigManager config = ConfigManager.getInstance();
        String prefix = "profile." + this.profile.name().toLowerCase();

        this.maxDepth = config.getIntProperty(prefix + ".max.depth", 4);
        this.maxPagesPerDomain = config.getIntProperty(prefix + ".max.pages.per.domain", 500);
        this.documentTypes = parseDocumentTypes(config.getProperty(prefix + ".content.types", "HTML,PDF,IMAGE"));
        this.followExternalLinks = config.getBooleanProperty(prefix + ".follow.external.links", true);
        this.maxExternalDepth = config.getIntProperty(prefix + ".max.external.depth", 1);
        this.detectApis = config.getBooleanProperty(prefix + ".detect.apis", true);
        this.detectDynamicContent = config.getBooleanProperty(prefix + ".detect.dynamic.content", true);
        this.apiScanDepth = config.getIntProperty(prefix + ".api.scan.depth", 3);
    }

    public CrawlConfig() {
        this(CrawlProfile.STANDARD);
    }

    private Set<DocumentType> parseDocumentTypes(String typesStr) {
        Set<DocumentType> types = new HashSet<>();
        if (typesStr != null) {
            Arrays.stream(typesStr.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(this::safeValueOfDocumentType)
                    .forEach(types::add);
        }
        return types;
    }

    private DocumentType safeValueOfDocumentType(String typeStr) {
        if ("HTML".equals(typeStr)) {
            return DocumentType.WEB_PAGE; // Conversion spécifique HTML → WEB_PAGE
        }
        try {
            return DocumentType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Type de document inconnu: " + typeStr + ", affectation à UNKNOWN");
            return DocumentType.UNKNOWN;
        }
    }
}