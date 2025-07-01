package com.smartcrawler.platform.impl;

import com.smartcrawler.platform.ServiceInstaller;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Installateur de service pour Windows (NSSM).
 * Génère et exécute un script batch pour installer le service.
 */
public class WindowsServiceInstaller implements ServiceInstaller {

    private final Logger logger;
    private static final String NSSM_PATH = "C:\\Program Files\\nssm\\nssm.exe";
    private static final String TEMPLATE_PATH = "service-templates/nssm-install.bat.template";
    private static final String SCRIPT_PATH = "C:\\Windows\\Temp\\install-smart-crawler.bat";

    public WindowsServiceInstaller() {
        this.logger = LoggerFactory.getLogger(WindowsServiceInstaller.class.getName());
    }

    /**
     * Installe le service Windows avec NSSM.
     *
     * @throws IOException en cas d'erreur d'écriture ou d'exécution
     */
    @Override
    public void install() throws IOException {
        try {
            String template = new String(getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH).readAllBytes());
            String scriptContent = template.replace("{{jarPath}}", "C:\\Program Files\\smart-crawler-agent\\smart-crawler-agent.jar");
            Files.write(Paths.get(SCRIPT_PATH), scriptContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Runtime.getRuntime().exec(NSSM_PATH + " install SmartCrawlerAgent " + SCRIPT_PATH);
            logger.info("Service NSSM installé avec succès", null);
        } catch (IOException e) {
            logger.error("Erreur lors de l'installation du service NSSM", e);
            throw e;
        }
    }
}