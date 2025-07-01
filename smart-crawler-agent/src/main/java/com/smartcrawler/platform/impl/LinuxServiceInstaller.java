package com.smartcrawler.platform.impl;

import com.smartcrawler.platform.ServiceInstaller;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Installateur de service pour Linux (systemd).
 * Génère et installe un fichier unit systemd.
 */
public class LinuxServiceInstaller implements ServiceInstaller {

    private final Logger logger;
    private static final String SYSTEMD_PATH = "/etc/systemd/system/smart-crawler-agent.service";
    private static final String TEMPLATE_PATH = "service-templates/systemd.service.template";

    public LinuxServiceInstaller() {
        this.logger = LoggerFactory.getLogger(LinuxServiceInstaller.class.getName());
    }

    /**
     * Installe le service systemd sur Linux.
     *
     * @throws IOException en cas d'erreur d'écriture ou de lecture
     */
    @Override
    public void install() throws IOException {
        try {
            String template = new String(getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH).readAllBytes());
            String serviceContent = template.replace("{{jarPath}}", "/usr/local/bin/smart-crawler-agent.jar");
            Files.write(Paths.get(SYSTEMD_PATH), serviceContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Runtime.getRuntime().exec("systemctl enable smart-crawler-agent.service");
            Runtime.getRuntime().exec("systemctl start smart-crawler-agent.service");
            logger.info("Service systemd installé avec succès: " + SYSTEMD_PATH, null);
        } catch (IOException e) {
            logger.error("Erreur lors de l'installation du service systemd", e);
            throw e;
        }
    }
}