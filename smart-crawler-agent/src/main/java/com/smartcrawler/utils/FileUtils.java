package com.smartcrawler.utils;

import com.smartcrawler.core.ConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Utilitaires pour les opérations sur fichiers.
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class.getName());
    private static final ConfigManager configManager = ConfigManager.getInstance();

    /**
     * Écrit du contenu dans un fichier.
     *
     * @param filePath Chemin du fichier
     * @param content  Contenu à écrire
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void writeToFile(String filePath, String content) throws IOException {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Content written to file: " + filePath, null);
        } catch (IOException e) {
            logger.error("Error writing to file: " + filePath, e);
            throw e;
        }
    }

    /**
     * Lit le contenu d'un fichier.
     *
     * @param filePath Chemin du fichier
     * @return Contenu du fichier
     * @throws IOException en cas d'erreur de lecture
     */
    public static String readFromFile(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath);
            String content = Files.readString(path);
            logger.debug("Content read from file: " + filePath, null);
            return content;
        } catch (IOException e) {
            logger.error("Error reading from file: " + filePath, e);
            throw e;
        }
    }

    /**
     * Écrit un rapport de crawl dans le fichier configuré.
     *
     * @param report Contenu du rapport
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void writeCrawlReport(String report) throws IOException {
        String reportFile = configManager.getProperty("logging.report.file", "reports/crawl-report.json");
        writeToFile(reportFile, report);
    }
}