package com.smartcrawler.crawler;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.CrawlConfig;
import com.smartcrawler.model.CrawlResult;
import com.smartcrawler.model.enums.CrawlStatus;
import com.smartcrawler.model.enums.DocumentType;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.ValidationUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Crawler principal pour les pages HTML/statiques, médias, PDF, et autres documents.
 * Utilise Jsoup pour parser le contenu, extraire les liens, médias, et respecter les contraintes de configuration.
 */
public class WebCrawler {

    private final Logger logger;
    private final ContentExtractor contentExtractor;
    private final Set<String> visitedUrls;
    // Regex pour vérifier si un nom de fichier est linguistiquement pertinent
    private static final Pattern MEANINGFUL_NAME_PATTERN = Pattern.compile("[a-zA-Z]{3,}");
    // Extensions pour identifier les documents non-HTML
    private static final Set<String> document_extensions = Set.of(".pdf", ".docx", ".txt");

    public WebCrawler() {
        this.logger = LoggerFactory.getLogger(WebCrawler.class.getName());
        this.contentExtractor = new ContentExtractor();
        this.visitedUrls = new HashSet<>();
    }

    /**
     * Crawle une URL donnée selon la configuration fournie.
     * Inclut le traitement des médias et PDF comme des documents indépendants.
     *
     * @param config Configuration du crawl
     * @param url    URL à crawler
     * @param depth  Profondeur actuelle du crawl
     * @return Résultat du crawl pour la page principale
     */
    public CrawlResult crawl(CrawlConfig config, String url, int depth) {
        if (!ValidationUtils.isValidUrl(url)) {
            logger.error("URL invalide : " + url, null);
            return createErrorResult(url, "URL invalide", CrawlStatus.ERROR);
        }

        if (depth > config.getMaxDepth() || visitedUrls.size() >= config.getMaxPagesPerDomain()) {
            logger.info("Limite de profondeur ou de pages atteinte pour : " + url, null);
            return createErrorResult(url, "Limite atteinte", CrawlStatus.BLOCKED);
        }

        if (visitedUrls.contains(url)) {
            logger.debug("URL déjà visitée : " + url, null);
            return createErrorResult(url, "URL déjà visitée", CrawlStatus.BLOCKED);
        }

        visitedUrls.add(url);
        logger.info("Début du crawl pour : " + url + " (profondeur : " + depth + ")", null);

        try {
            long start = System.currentTimeMillis();

            Document doc = Jsoup.connect(url)
                    .userAgent(getUserAgent(config))
                    .timeout(ConfigManager.getInstance().getIntProperty("crawler.request.timeout", 10000))
                    .get();

            long end = System.currentTimeMillis();

            CrawlResult result = new CrawlResult();
            result.setUrl(url);
            result.setStatus(CrawlStatus.SUCCESS);
            result.setDocumentType(DocumentType.WEB_PAGE);
            result.setTitle(doc.title());
            result.setDescription(getMetaDescription(doc));
            result.setContent(contentExtractor.extractTextContent(doc));
            result.setLinks(extractLinks(doc, config, doc.location()));
            result.setMediaUrls(extractMediaUrls(doc, config));
            result.setCrawlTimestamp(System.currentTimeMillis());
            result.setHttpStatus(200);
            result.setResponseTime(end - start);

            logger.info("Crawl réussi pour : " + url, null);
            return result;
        } catch (IOException e) {
            if (e instanceof org.jsoup.HttpStatusException) {
                org.jsoup.HttpStatusException httpEx = (org.jsoup.HttpStatusException) e;
                if (httpEx.getStatusCode() == 404) {
                    return createErrorResult(url, "Page non trouvée (404)", CrawlStatus.ERROR);
                }
            }

            logger.error("Erreur lors du crawl de : " + url, e);
            return createErrorResult(url, e.getMessage(), CrawlStatus.ERROR);
        }
    }

    private String getUserAgent(CrawlConfig config) {
        return ConfigManager.getInstance().getProperty("crawler.user.agent", "SmartCrawler/1.0");
    }

    private String getMetaDescription(Document doc) {
        Elements meta = doc.select("meta[name=description]");
        return meta.isEmpty() ? "" : meta.first().attr("content");
    }

    private Set<String> extractLinks(Document doc, CrawlConfig config, String currentUrl) {
        Set<String> links = new HashSet<>();
        Elements elements = doc.select("a[href]");
        for (Element element : elements) {
            String href = element.absUrl("href");
            if (ValidationUtils.isValidUrl(href) && isAllowedLink(href, config, currentUrl)) {
                links.add(href);
            }
        }
        return links;
    }

    private boolean isAllowedLink(String href, CrawlConfig config, String currentUrl) {
        if (!config.isFollowExternalLinks()) {
            try {
                String baseDomain = new java.net.URL(currentUrl).getHost();
                String targetDomain = new java.net.URL(href).getHost();
                return baseDomain.equalsIgnoreCase(targetDomain);
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification du domaine : " + href, e);
                return false;
            }
        }
        return true;
    }

    private Set<String> extractMediaUrls(Document doc, CrawlConfig config) {
        Set<String> mediaUrls = new HashSet<>();

        if (config.getDocumentTypes().contains(DocumentType.IMAGE)) {
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                mediaUrls.add(img.absUrl("src"));
            }
        }

        if (config.getDocumentTypes().contains(DocumentType.VIDEO)) {
            Elements videos = doc.select("video[src], source[src]");
            for (Element video : videos) {
                mediaUrls.add(video.absUrl("src"));
            }
        }

        if (config.getDocumentTypes().contains(DocumentType.AUDIO)) {
            Elements audios = doc.select("audio[src]");
            for (Element audio : audios) {
                mediaUrls.add(audio.absUrl("src"));
            }
        }

        return mediaUrls;
    }

    /**
     * Extrait les documents (PDF, médias, autres) comme des CrawlResult indépendants.
     *
     * @param doc        Document HTML
     * @param config     Configuration du crawl
     * @param currentUrl URL de la page actuelle
     * @return Liste des résultats des documents
     */
    public List<CrawlResult> extractDocumentResults(Document doc, CrawlConfig config, String currentUrl) {
        List<CrawlResult> documentResults = new ArrayList<>();

        // Traiter les PDF et autres documents
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.absUrl("href");
            if (!ValidationUtils.isValidUrl(href)) {
                continue;
            }
            String lowerHref = href.toLowerCase();
            DocumentType type = null;
            if (lowerHref.endsWith(".pdf")) {
                type = DocumentType.PDF;
            } else if (document_extensions.contains(lowerHref.substring(lowerHref.lastIndexOf('.')))) {
                type = DocumentType.DOCUMENT;
            }
            if (type != null && config.getDocumentTypes().contains(type)) {
                CrawlResult docResult = crawlDocumentItem(href, type, link, new HashSet<>(), config);
                if (docResult != null) {
                    documentResults.add(docResult);
                }
            }
        }

        // Traiter les images
        if (config.getDocumentTypes().contains(DocumentType.IMAGE)) {
            Elements images = doc.select("img[src], picture source[srcset]");
            for (Element img : images) {
                String mainUrl = img.hasAttr("src") ? img.absUrl("src") : "";
                Set<String> variants = new HashSet<>();
                if (img.tagName().equals("source") && img.hasAttr("srcset")) {
                    String[] srcset = img.attr("srcset").split(",");
                    for (String src : srcset) {
                        String variantUrl = src.trim().split("\\s+")[0];
                        if (ValidationUtils.isValidUrl(variantUrl)) {
                            variants.add(variantUrl);
                        }
                    }
                    if (mainUrl.isEmpty() && !variants.isEmpty()) {
                        mainUrl = variants.iterator().next();
                        variants.remove(mainUrl);
                    }
                }
                if (ValidationUtils.isValidUrl(mainUrl)) {
                    CrawlResult mediaResult = crawlDocumentItem(mainUrl, DocumentType.IMAGE, img, variants, config);
                    if (mediaResult != null) {
                        documentResults.add(mediaResult);
                    }
                }
            }
        }

        // Traiter les vidéos
        if (config.getDocumentTypes().contains(DocumentType.VIDEO)) {
            Elements videos = doc.select("video[src], video source[src]");
            for (Element video : videos) {
                String mainUrl = video.absUrl("src");
                Set<String> variants = new HashSet<>();
                if (video.tagName().equals("source") && video.hasAttr("src")) {
                    variants.add(mainUrl);
                    mainUrl = video.parent().absUrl("src");
                }
                if (ValidationUtils.isValidUrl(mainUrl)) {
                    CrawlResult mediaResult = crawlDocumentItem(mainUrl, DocumentType.VIDEO, video, variants, config);
                    if (mediaResult != null) {
                        documentResults.add(mediaResult);
                    }
                }
            }
        }

        // Traiter les audios
        if (config.getDocumentTypes().contains(DocumentType.AUDIO)) {
            Elements audios = doc.select("audio[src], audio source[src]");
            for (Element audio : audios) {
                String mainUrl = audio.absUrl("src");
                Set<String> variants = new HashSet<>();
                if (audio.tagName().equals("source") && audio.hasAttr("src")) {
                    variants.add(mainUrl);
                    mainUrl = audio.parent().absUrl("src");
                }
                if (ValidationUtils.isValidUrl(mainUrl)) {
                    CrawlResult mediaResult = crawlDocumentItem(mainUrl, DocumentType.AUDIO, audio, variants, config);
                    if (mediaResult != null) {
                        documentResults.add(mediaResult);
                    }
                }
            }
        }

        return documentResults;
    }

    /**
     * Crawle un document individuel (PDF, média, etc.) et crée un CrawlResult.
     *
     * @param url        URL principale du document
     * @param type       Type de document
     * @param element    Élément HTML associé
     * @param variants   URLs des variantes
     * @param config     Configuration du crawl
     * @return CrawlResult pour le document, ou null si invalide
     */
    private CrawlResult crawlDocumentItem(String url, DocumentType type, Element element, Set<String> variants, CrawlConfig config) {
        try {
            // Vérifier l'accessibilité et le type MIME
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(ConfigManager.getInstance().getIntProperty("crawler.document.timeout", 5000));
            conn.setRequestProperty("User-Agent", getUserAgent(config));
            conn.connect();

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                logger.debug("Document inaccessible (code " + statusCode + ") : " + url, null);
                return null;
            }

            String contentType = conn.getContentType();
            if (!isValidDocumentType(contentType, type)) {
                logger.debug("Type MIME invalide pour " + type + " : " + contentType + " (" + url + ")", null);
                return null;
            }

            // Créer le CrawlResult
            CrawlResult result = new CrawlResult();
            result.setUrl(url);
            result.setStatus(CrawlStatus.SUCCESS);
            result.setDocumentType(type);
            result.setHttpStatus(statusCode);
            result.setCrawlTimestamp(System.currentTimeMillis());
            result.setLinks(variants);

            // Extraire le titre
            String title = element.attr("title");
            if (title.isEmpty() && element.tagName().equals("a")) {
                title = element.text().trim();
            }
            if (title.isEmpty() && type == DocumentType.IMAGE) {
                title = element.attr("alt");
            }
            if (title.isEmpty()) {
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                if (isMeaningfulName(fileName)) {
                    title = fileName.replaceAll("[_-]", " ").replaceAll("\\..+$", "");
                }
            }
            result.setTitle(title);

            // Extraire la description
            String description = element.attr("data-description");
            if (description.isEmpty() && type == DocumentType.IMAGE) {
                description = element.attr("alt");
            }
            result.setDescription(description);

            // Extraire le contenu
            if (type == DocumentType.PDF || type == DocumentType.DOCUMENT) {
                try {
                    String docContent = contentExtractor.extractTextContentFromUrl(url);
                    result.setContent(docContent);
                } catch (Exception e) {
                    logger.debug("Impossible d'extraire le contenu du document : " + url, e);
                }
            } else if (type == DocumentType.VIDEO || type == DocumentType.AUDIO) {
                Elements tracks = element.select("track[kind=subtitles][src]");
                if (!tracks.isEmpty()) {
                    String subtitleUrl = tracks.first().absUrl("src");
                    if (ValidationUtils.isValidUrl(subtitleUrl)) {
                        try {
                            String subtitles = Jsoup.connect(subtitleUrl)
                                    .userAgent(getUserAgent(config))
                                    .timeout(ConfigManager.getInstance().getIntProperty("crawler.request.timeout", 10000))
                                    .get().text();
                            result.setContent(subtitles);
                        } catch (IOException e) {
                            logger.debug("Impossible de récupérer les sous-titres pour : " + subtitleUrl, e);
                        }
                    }
                }
            }

            logger.info("Document crawlé avec succès : " + url, null);
            return result;
        } catch (IOException e) {
            logger.debug("Erreur lors du crawl du document : " + url, e);
            return null;
        }
    }

    /**
     * Vérifie si le type MIME correspond au type de document attendu.
     *
     * @param contentType Type MIME
     * @param type        Type de document
     * @return Vrai si valide
     */
    private boolean isValidDocumentType(String contentType, DocumentType type) {
        if (contentType == null) {
            return false;
        }
        switch (type) {
            case PDF:
                return contentType.equals("application/pdf");
            case IMAGE:
                return contentType.startsWith("image/");
            case VIDEO:
                return contentType.startsWith("video/");
            case AUDIO:
                return contentType.startsWith("audio/");
            case DOCUMENT:
                return contentType.startsWith("application/") || contentType.startsWith("text/");
            default:
                return false;
        }
    }

    /**
     * Vérifie si le nom de fichier est linguistiquement pertinent.
     *
     * @param fileName Nom du fichier
     * @return Vrai si pertinent
     */
    private boolean isMeaningfulName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String nameWithoutExt = fileName.replaceAll("\\..+$", "");
        return MEANINGFUL_NAME_PATTERN.matcher(nameWithoutExt).find();
    }

    private CrawlResult createErrorResult(String url, String errorMessage, CrawlStatus status) {
        CrawlResult result = new CrawlResult();
        result.setUrl(url);
        result.setStatus(status);
        result.setDocumentType(DocumentType.UNKNOWN);
        result.setErrorMessage(errorMessage);
        result.setCrawlTimestamp(System.currentTimeMillis());
        return result;
    }
}