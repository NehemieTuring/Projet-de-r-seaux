package com.smartcrawler.ui;

import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.core.CrawlerEngine;
import com.smartcrawler.core.SchedulerService;
import com.smartcrawler.service.HealthCheckService;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import com.smartcrawler.utils.SystemTrayManager;
import com.auth.AuthenticationConfig;
import com.auth.AuthenticationService;
import com.smartcrawler.service.BackendClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class UIManager {
    private static final Logger logger = LoggerFactory.getLogger(UIManager.class.getName());

    @Getter
    @Setter
    private Stage primaryStage;
    @Setter
    private MainController mainController;
    @Setter
    private AuthController authController;
    private final SystemTrayManager systemTrayManager;
    private final HealthCheckService healthCheckService;
    @Getter
    private final SchedulerService schedulerService;
    private final CrawlerEngine crawlerEngine;
    private final AuthenticationService authService;
    @Setter
    @Getter
    private ServiceState serviceState = ServiceState.STOPPED;

    public UIManager(SchedulerService schedulerService, CrawlerEngine crawlerEngine) {
        this.schedulerService = schedulerService;
        this.crawlerEngine = crawlerEngine;
        this.healthCheckService = new HealthCheckService();
        this.systemTrayManager = new SystemTrayManager(this);
        AuthenticationConfig authConfig = new AuthenticationConfig(ConfigManager.getInstance());
        this.authService = new AuthenticationService(authConfig, new BackendClient());
    }

    public void showTermsOfUse() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartcrawler/smartcrawleragent/terms-of-use.fxml"));
            Parent root = loader.load();
            TermsOfUseController controller = loader.getController();
            controller.setUIManager(this);

            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles/terms-of-use.css").toExternalForm());

            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/smartcrawler/smartcrawleragent/images/ico.png")));
            primaryStage.setTitle("Smart Crawler Agent - Terms of Use");
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Terms of Use screen displayed", null);
        } catch (IOException e) {
            logger.error("Error loading terms of use FXML", e);
            Platform.exit();
        }
    }

    public void showMainInterface() {
//        if (!isAuthenticated()) {
//            showAuthenticationInterface();
//            return;
//        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartcrawler/smartcrawleragent/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setUIManager(this);
            setMainController(controller);

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/smartcrawler/smartcrawleragent/images/ico.png")));
            primaryStage.setTitle("Smart Crawler Agent");
            primaryStage.setScene(scene);
            primaryStage.show();
            logger.info("Main interface displayed", null);
        } catch (IOException e) {
            logger.error("Error loading main FXML", e);
            Platform.exit();
        }
    }

    public void showAuthenticationInterface() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smartcrawler/smartcrawleragent/auth.fxml"));
            Parent root = loader.load();
            AuthController controller = loader.getController();
            controller.setUIManager(this);
            setAuthController(controller);

            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/auth.css").toExternalForm());
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/smartcrawler/smartcrawleragent/images/ico.png")));
            primaryStage.setTitle("Smart Crawler Agent - Authentication");
            primaryStage.setScene(scene);
            primaryStage.show();
            logger.info("Authentication interface displayed", null);
        } catch (IOException e) {
            logger.error("Error loading auth FXML", e);
            Platform.exit();
        }
    }

    public void showProfileInterface() {
        showAuthenticationInterface();
        if (authController != null) {
            Platform.runLater(() -> authController.selectProfileTab());
        }
    }

    private boolean isAuthenticated() {
        Properties userConfig = new Properties();
        try {
            userConfig.load(Files.newInputStream(Paths.get("user-config.properties")));
            String uuid = userConfig.getProperty("auth.uuid");
            String apiKey = userConfig.getProperty("auth.apiKey");
            if (uuid == null || apiKey == null || uuid.isEmpty() || apiKey.isEmpty()) {
                logger.info("Not authenticated: missing UUID or API key", null);
                return false;
            }
            boolean isValid = authService.validateToken(uuid, apiKey);
            logger.info("Authentication check: " + (isValid ? "Authenticated" : "Invalid token"), null);
            return isValid;
        } catch (Exception e) {
            logger.debug("Error checking authentication, assuming not authenticated", e);
            return false;
        }
    }

    public void logout() {
        try {
            Properties userConfig = new Properties();
            if (Files.exists(Paths.get("user-config.properties"))) {
                userConfig.load(Files.newInputStream(Paths.get("user-config.properties")));
            }
            userConfig.remove("auth.uuid");
            userConfig.remove("auth.apiKey");
            userConfig.store(Files.newOutputStream(Paths.get("user-config.properties")), "User configuration");
            logger.info("Logged out successfully", null);
            showAuthenticationInterface();
        } catch (IOException e) {
            logger.error("Error during logout", e);
        }
    }

    public void startServices() {
        if (serviceState == ServiceState.RUNNING) {
            logger.warn("Services already running", null);
            return;
        }

        try {
            setServiceState(ServiceState.RUNNING);
            healthCheckService.start();
            schedulerService.setCrawlResultListener(payload -> {
                if (mainController != null) {
                    mainController.updateHistory(payload);
                }
            });
            schedulerService.startScheduler();
            systemTrayManager.initSystemTray();
            logger.info("All services started successfully", null);
        } catch (Exception e) {
            setServiceState(ServiceState.ERROR);
            logger.error("Failed to start services", e);
            Platform.runLater(() -> showErrorAlert("Erreur", "Échec du démarrage des services : " + e.getMessage()));
        }
    }

    public void stopServices(boolean exitApplication) {
        if (serviceState != ServiceState.RUNNING) {
            logger.warn("Services not running", null);
            if (exitApplication) {
                Platform.exit();
            }
            return;
        }

        try {
            setServiceState(ServiceState.STOPPED);
            healthCheckService.stop();
            schedulerService.shutdown();
            crawlerEngine.shutdown();
            systemTrayManager.removeSystemTray();
            logger.info("All services stopped successfully", null);
        } catch (Exception e) {
            setServiceState(ServiceState.ERROR);
            logger.error("Failed to stop services", e);
            Platform.runLater(() -> showErrorAlert("Erreur", "Échec de l'arrêt des services : " + e.getMessage()));
        } finally {
            if (exitApplication) {
                Platform.exit();
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public enum ServiceState {
        RUNNING,
        STOPPED,
        ERROR
    }
}