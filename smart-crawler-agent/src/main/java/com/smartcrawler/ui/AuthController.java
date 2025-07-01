package com.smartcrawler.ui;

import com.auth.AuthenticationConfig;
import com.auth.AuthenticationService;
import com.smartcrawler.core.ConfigManager;
import com.smartcrawler.model.ClientCredentials;
import com.smartcrawler.service.BackendClient;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class.getName());
    private static final long SESSION_TIMEOUT_MINUTES;
    static {
        long timeoutValue;
        String timeout = ConfigManager.getInstance().getProperty("auth.session.timeout", "30");
        try {
            timeoutValue = Long.parseLong(timeout);
        } catch (NumberFormatException e) {
            logger.error("Invalid session timeout value: " + timeout + ", defaulting to 30 minutes", e);
            timeoutValue = 30;
        }
        SESSION_TIMEOUT_MINUTES = timeoutValue;
    }


    @FXML private VBox root;
    @FXML private MenuBar menuBar;
    @FXML private MenuItem registerMenuItem;
    @FXML private MenuItem loginMenuItem;
    @FXML private MenuItem profileMenuItem;
    @FXML private MenuItem logoutMenuItem;
    @FXML private TabPane authTabPane;
    @FXML private Tab registerTab;
    @FXML private Label loginResultLabel;
    @FXML private Tab loginTab;
    @FXML private Tab profileTab;
    @FXML private TextField registerOrganizationField;
    @FXML private TextField registerEmailField;
    @FXML private TextField registerContactNameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private TextField registerSecretPhraseField;
    @FXML private TextField registerHintField;
    @FXML private Button registerButton;
    @FXML private Label registerResultLabel;
    @FXML private TextField loginOrganizationField;
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private VBox forgotPasswordPane;
    @FXML private TextField forgotOrganizationField;
    @FXML private TextField forgotEmailField;
    @FXML private Label hintLabel;
    @FXML private TextField secretPhraseField;
    @FXML private Button verifySecretPhraseButton;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Button resetPasswordButton;
    @FXML private Label forgotResultLabel;
    @FXML private Label profileOrganizationLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileContactNameLabel;
    @FXML private TextField updateEmailField;
    @FXML private TextField updateContactNameField;
    @FXML private TextField updateOrganizationField;
    @FXML private PasswordField updateInfoPasswordField;
    @FXML private Button updateInfoButton;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newProfilePasswordField;
    @FXML private PasswordField confirmProfilePasswordField;
    @FXML private Button updatePasswordButton;
    @FXML private PasswordField deletePasswordField;
    @FXML private Button deleteAccountButton;
    @FXML private Label profileResultLabel;

    private UIManager uiManager;
    private AuthenticationService authService;
    private ClientCredentials currentCredentials;
    private ScheduledExecutorService sessionTimeoutExecutor;
    private long lastActivityTime;

    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
    }

    @FXML
    public void initialize() {
        AuthenticationConfig authConfig = new AuthenticationConfig(ConfigManager.getInstance());
        authService = new AuthenticationService(authConfig, new BackendClient());
        sessionTimeoutExecutor = new ScheduledThreadPoolExecutor(1);
        lastActivityTime = System.currentTimeMillis();
        updateMenuItems();
        loadProfile();
        startSessionTimeoutCheck();

        root.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                if (newValue != null) {
                    newValue.setOnMouseMoved(event -> updateLastActivityTime());
                    newValue.setOnKeyPressed(event -> updateLastActivityTime());
                }
            }
        });
    }

    private void startSessionTimeoutCheck() {
        sessionTimeoutExecutor.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastActivityTime > SESSION_TIMEOUT_MINUTES * 60 * 1000) {
                Platform.runLater(() -> {
                    logger.info("Session timed out due to inactivity", null);
                    handleLogout();
                });
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
    }

    @FXML
    private void showRegisterTab() {
        authTabPane.getSelectionModel().select(registerTab);
        updateMenuItems();
    }

    @FXML
    private void showLoginTab() {
        authTabPane.getSelectionModel().select(loginTab);
        forgotPasswordPane.setVisible(false);
        forgotPasswordPane.setManaged(false);
        updateMenuItems();
    }

    @FXML
    private void showProfileTab() {
        if (!isAuthenticated()) {
            showLoginTab();
            return;
        }
        authTabPane.getSelectionModel().select(profileTab);
        loadProfile();
        updateMenuItems();
    }

    @FXML
    private void handleRegister() {
        try {
            if (!registerPasswordField.getText().equals(registerConfirmPasswordField.getText())) {
                registerResultLabel.setText("Les mots de passe ne correspondent pas");
                registerResultLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(registerOrganizationField.getText());
            credentials.setEmail(registerEmailField.getText());
            credentials.setContactName(registerContactNameField.getText());
            credentials.setPassword(registerPasswordField.getText());
            credentials.setSecretPhrase(registerSecretPhraseField.getText());
            credentials.setHint(registerHintField.getText());

            ClientCredentials result = authService.register(credentials);
            saveCredentials(result);
            currentCredentials = result;
            registerResultLabel.setText("Inscription réussie !");
            registerResultLabel.setStyle("-fx-text-fill: green;");
            uiManager.showMainInterface();
        } catch (Exception e) {
            registerResultLabel.setText("Erreur: " + e.getMessage());
            registerResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Registration error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleLogin() {
        try {
            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(loginOrganizationField.getText());
            credentials.setEmail(loginEmailField.getText());
            credentials.setPassword(loginPasswordField.getText());

            ClientCredentials result = authService.authenticate(credentials);
            saveCredentials(result);
            currentCredentials = result;
            loginResultLabel.setText("Connexion réussie !");
            loginResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
            uiManager.showMainInterface();
        } catch (Exception e) {
            loginResultLabel.setText("Erreur: " + e.getMessage());
            loginResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Authentication error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void showForgotPassword() {
        forgotPasswordPane.setVisible(true);
        forgotPasswordPane.setManaged(true);
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);
        confirmNewPasswordField.setVisible(false);
        confirmNewPasswordField.setManaged(false);
        resetPasswordButton.setVisible(false);
        resetPasswordButton.setManaged(false);
        hintLabel.setText("Indice: (saisissez l'organisation et l'email pour voir l'indice)");
        updateLastActivityTime();
    }

    @FXML
    private void verifySecretPhrase() {
        try {
            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(forgotOrganizationField.getText());
            credentials.setEmail(forgotEmailField.getText());
            credentials.setSecretPhrase(secretPhraseField.getText());

            if (hintLabel.getText().equals("Indice: (saisissez l'organisation et l'email pour voir l'indice)")) {
                credentials.setPassword("dummy");
                authService.resetCredentials(credentials, null);
                hintLabel.setText("Indice: " + credentials.getHint());
                forgotResultLabel.setText("Vérifiez la phrase secrète");
                forgotResultLabel.setStyle("-fx-text-fill: blue;");
                updateLastActivityTime();
                return;
            }

            authService.resetCredentials(credentials, null);
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            confirmNewPasswordField.setVisible(true);
            confirmNewPasswordField.setManaged(true);
            resetPasswordButton.setVisible(true);
            resetPasswordButton.setManaged(true);
            forgotResultLabel.setText("Phrase correcte, entrez le nouveau mot de passe");
            forgotResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
        } catch (Exception e) {
            forgotResultLabel.setText("Erreur: " + e.getMessage());
            forgotResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Secret phrase verification error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleResetPassword() {
        try {
            if (!newPasswordField.getText().equals(confirmNewPasswordField.getText())) {
                forgotResultLabel.setText("Les mots de passe ne correspondent pas");
                forgotResultLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(forgotOrganizationField.getText());
            credentials.setEmail(forgotEmailField.getText());
            credentials.setPassword(newPasswordField.getText());
            credentials.setSecretPhrase(secretPhraseField.getText());

            ClientCredentials result = authService.resetCredentials(credentials, null);
            saveCredentials(result);
            currentCredentials = result;
            forgotResultLabel.setText("Mot de passe réinitialisé avec succès !");
            forgotResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
            uiManager.showMainInterface();
        } catch (Exception e) {
            forgotResultLabel.setText("Erreur: " + e.getMessage());
            forgotResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Password reset error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleUpdateInfo() {
        try {
            if (!isAuthenticated()) {
                profileResultLabel.setText("Session expirée, reconnectez-vous");
                profileResultLabel.setStyle("-fx-text-fill: red;");
                showLoginTab();
                return;
            }

            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(updateOrganizationField.getText().isEmpty() ? currentCredentials.getOrganization() : updateOrganizationField.getText());
            credentials.setEmail(updateEmailField.getText().isEmpty() ? currentCredentials.getEmail() : updateEmailField.getText());
            credentials.setContactName(updateContactNameField.getText().isEmpty() ? currentCredentials.getContactName() : updateContactNameField.getText());

            ClientCredentials result = authService.updateAccount(credentials, updateInfoPasswordField.getText());
            saveCredentials(result);
            currentCredentials = result;
            profileResultLabel.setText("Informations mises à jour avec succès !");
            profileResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
            loadProfile();
        } catch (Exception e) {
            profileResultLabel.setText("Erreur: " + e.getMessage());
            profileResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Info update error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleUpdatePassword() {
        try {
            if (!isAuthenticated()) {
                profileResultLabel.setText("Session expirée, reconnectez-vous");
                profileResultLabel.setStyle("-fx-text-fill: red;");
                showLoginTab();
                return;
            }

            if (!newProfilePasswordField.getText().equals(confirmProfilePasswordField.getText())) {
                profileResultLabel.setText("Les mots de passe ne correspondent pas");
                profileResultLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            ClientCredentials credentials = new ClientCredentials();
            credentials.setOrganization(currentCredentials.getOrganization());
            credentials.setEmail(currentCredentials.getEmail());
            credentials.setContactName(currentCredentials.getContactName());
            credentials.setPassword(newProfilePasswordField.getText());

            ClientCredentials result = authService.resetCredentials(credentials, oldPasswordField.getText());
            saveCredentials(result);
            currentCredentials = result;
            profileResultLabel.setText("Mot de passe mis à jour avec succès !");
            profileResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
        } catch (Exception e) {
            profileResultLabel.setText("Erreur: " + e.getMessage());
            profileResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Password update error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleDeleteAccount() {
        try {
            if (!isAuthenticated()) {
                profileResultLabel.setText("Session expirée, reconnectez-vous");
                profileResultLabel.setStyle("-fx-text-fill: red;");
                showLoginTab();
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le compte");
            confirmation.setContentText("Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.");
            if (confirmation.showAndWait().get() != ButtonType.OK) {
                return;
            }

            authService.deleteAccount(currentCredentials.getUuid().toString(), currentCredentials.getApiKey(), deletePasswordField.getText());
            uiManager.logout();
            currentCredentials = null;
            profileResultLabel.setText("Compte supprimé avec succès !");
            profileResultLabel.setStyle("-fx-text-fill: green;");
            updateLastActivityTime();
            showLoginTab();
        } catch (Exception e) {
            profileResultLabel.setText("Erreur: " + e.getMessage());
            profileResultLabel.setStyle("-fx-text-fill: red;");
            logger.error("Account deletion error: " + e.getMessage(), e);
        }
    }

    @FXML
    private void handleLogout() {
        uiManager.logout();
        currentCredentials = null;
        updateMenuItems();
        showLoginTab();
        updateLastActivityTime();
    }

    private void loadProfile() {
        if (currentCredentials != null && isAuthenticated()) {
            profileOrganizationLabel.setText("Organisation: " + currentCredentials.getOrganization());
            profileEmailLabel.setText("Email: " + currentCredentials.getEmail());
            profileContactNameLabel.setText("Nom du contact: " + currentCredentials.getContactName());
        } else {
            Properties userConfig = loadUserConfig();
            String uuid = userConfig.getProperty("auth.uuid");
            String apiKey = userConfig.getProperty("auth.apiKey");
            if (uuid != null && apiKey != null) {
                try {
                    currentCredentials = new ClientCredentials();
                    currentCredentials.setUuid(UUID.fromString(uuid));
                    currentCredentials.setApiKey(apiKey);
                    profileOrganizationLabel.setText("Organisation: (non chargé)");
                    profileEmailLabel.setText("Email: (non chargé)");
                    profileContactNameLabel.setText("Nom du contact: (non chargé)");
                } catch (Exception e) {
                    logger.error("Error loading profile", e);
                }
            }
        }
    }

    private void saveCredentials(ClientCredentials credentials) {
        try {
            Properties userConfig = loadUserConfig();
            userConfig.setProperty("auth.uuid", credentials.getUuid().toString());
            userConfig.setProperty("auth.apiKey", credentials.getApiKey());
            userConfig.store(Files.newOutputStream(Paths.get("user-config.properties")), "User configuration");
            logger.info("Credentials saved to user-config.properties", null);
        } catch (IOException e) {
            logger.error("Error saving credentials", e);
        }
    }

    private Properties loadUserConfig() {
        Properties userConfig = new Properties();
        try {
            if (Files.exists(Paths.get("user-config.properties"))) {
                userConfig.load(Files.newInputStream(Paths.get("user-config.properties")));
            }
        } catch (IOException e) {
            logger.error("Error loading user-config.properties", e);
        }
        return userConfig;
    }

    private void updateMenuItems() {
        boolean isAuthenticated = currentCredentials != null && isAuthenticated();
        registerMenuItem.setDisable(isAuthenticated);
        loginMenuItem.setDisable(isAuthenticated);
        profileMenuItem.setDisable(!isAuthenticated);
        logoutMenuItem.setDisable(!isAuthenticated);
    }

    private boolean isAuthenticated() {
        Properties userConfig = loadUserConfig();
        String uuid = userConfig.getProperty("auth.uuid");
        String apiKey = userConfig.getProperty("auth.apiKey");
        try {
            return authService.validateToken(uuid, apiKey);
        } catch (Exception e) {
            logger.debug("Token validation failed: " + e.getMessage(), e);
            return false;
        }
    }

    public void selectProfileTab() {
        showProfileTab();
    }

    public void shutdown() {
        if (sessionTimeoutExecutor != null) {
            sessionTimeoutExecutor.shutdown();
            try {
                if (!sessionTimeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    sessionTimeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                sessionTimeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                logger.error("Interrupted during session timeout executor shutdown", e);
            }
        }
    }
}