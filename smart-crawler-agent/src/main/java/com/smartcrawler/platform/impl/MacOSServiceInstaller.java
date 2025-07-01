package com.smartcrawler.platform.impl;

import com.smartcrawler.platform.ServiceInstaller;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Installateur de service pour macOS (launchd).
 * Génère et installe un fichier plist pour launchd.
 */
public class MacOSServiceInstaller implements ServiceInstaller {

    private final Logger logger;
    private static final String PLIST_PATH = "/Library/LaunchDaemons/com.smartcrawler.agent.plist";
    private static final String TEMPLATE_PATH = "service-templates/launchd.plist.template";

    public MacOSServiceInstaller() {
        this.logger = LoggerFactory.getLogger(MacOSServiceInstaller.class.getName());
    }

    /**
     * Installe le service launchd sur macOS.
     *
     * @throws IOException en cas d'erreur d'écriture ou d'exécution
     */
    @Override
    public void install() throws IOException {
        try {
            String template = new String(getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH).readAllBytes());
            String plistContent = template.replace("{{jarPath}}", "/usr/local/bin/smart-crawler-agent.jar");
            Files.write(Paths.get(PLIST_PATH), plistContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Runtime.getRuntime().exec("launchctl load " + PLIST_PATH);
            logger.info("Service launchd installé avec succès: " + PLIST_PATH, null);
        } catch (IOException e) {
            logger.error("Erreur lors de l'installation du service launchd", e);
            throw e;
        }
    }
}