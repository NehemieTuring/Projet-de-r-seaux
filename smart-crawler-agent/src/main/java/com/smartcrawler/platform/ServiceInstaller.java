package com.smartcrawler.platform;

import java.io.IOException;

/**
 * Interface pour l'installation des services système.
 */
public interface ServiceInstaller {
    /**
     * Installe le service sur la plateforme cible.
     *
     * @throws IOException en cas d'erreur lors de l'installation
     */
    void install() throws IOException;
}