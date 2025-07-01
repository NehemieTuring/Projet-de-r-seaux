package com.smartcrawler.ui;

import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.core.SchedulerService;
import com.smartcrawler.core.CrawlerEngine;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application JavaFX principale pour Smart Crawler Agent.
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class.getName());
    private UIManager uiManager;

    // Instances partagées des services
    private static SchedulerService schedulerService;
    private static CrawlerEngine crawlerEngine;

    // Méthode pour initialiser les services depuis Main
    public static void initServices(SchedulerService scheduler, CrawlerEngine crawler) {
        schedulerService = scheduler;
        crawlerEngine = crawler;
    }

    @Override
    public void start(Stage primaryStage) {
        if (schedulerService == null || crawlerEngine == null) {
            logger.error("Services not initialized", null);
            showErrorAlert("Erreur", "Les services n'ont pas été initialisés. Veuillez redémarrer l'application.");
            Platform.exit();
            return;
        }

        uiManager = new UIManager(schedulerService, crawlerEngine);
        uiManager.setPrimaryStage(primaryStage);

        if (!areTermsAccepted()) {
            uiManager.showTermsOfUse();
        } else {
            uiManager.showMainInterface();
        }


        ConfigManager configManager = ConfigManager.getInstance();
        boolean autoStartServices = configManager.getBooleanProperty("services.auto.start", false);
        if (autoStartServices) {
            logger.info("Auto-starting services as configured", null);
            uiManager.startServices();
        }
    }

    private boolean areTermsAccepted() {
        Properties userConfig = new Properties();
        try {
            logger.info("Reading user-config.properties from: " + Paths.get("user-config.properties").toAbsolutePath(), null);
            userConfig.load(Files.newInputStream(Paths.get("user-config.properties")));
            String termsAccepted = userConfig.getProperty("terms.accepted", "false");
            logger.info("terms.accepted value: " + termsAccepted, null);
            return Boolean.parseBoolean(termsAccepted);
        } catch (IOException e) {
            logger.debug("No user-config.properties found, assuming terms not accepted", null);
            return false;
        }
    }

    @Override
    public void stop() {
        if (uiManager != null) {
            uiManager.stopServices(true);
        }
        logger.info("Application stopped", null);
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
            alert.showAndWait();
        });
    }
}