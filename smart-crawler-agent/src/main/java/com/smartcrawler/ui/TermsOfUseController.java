package com.smartcrawler.ui;

import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Contrôleur pour l'écran des conditions d'utilisation.
 */
public class TermsOfUseController {

    private static final Logger logger = LoggerFactory.getLogger(TermsOfUseController.class.getName());
    private UIManager uiManager;

    @FXML
    private TextArea termsTextArea;
    @FXML
    private CheckBox acceptCheckBox;
    @FXML
    private javafx.scene.control.Button acceptButton;

    @FXML
    private void initialize() {
        termsTextArea.setText("Smart Crawler Agent Terms of Use\n\n" +
                "By using this software, you agree to:\n" +
                "- Run the application in the background.\n" +
                "- Allow automatic startup on system boot.\n" +
                "- Respect website terms and robots.txt.\n" +
                "- Use the application responsibly.\n\n" +
                "This software is provided 'as is' without warranties.");
        acceptButton.setDisable(true);
    }

    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
    }

    @FXML
    private void onAcceptCheckBox() {
        acceptButton.setDisable(!acceptCheckBox.isSelected());
    }

    @FXML
    private void acceptTerms() {
        saveTermsAcceptance();
        uiManager.getPrimaryStage().close();
        uiManager.getPrimaryStage().close(); // si tu veux fermer la fenêtre actuelle
        uiManager.showMainInterface();
        logger.info("Terms of use accepted", null);
    }

    private void saveTermsAcceptance() {
        Properties userConfig = new Properties();
        userConfig.setProperty("terms.accepted", "true");
        try {
            userConfig.store(Files.newOutputStream(Paths.get("user-config.properties")), "User configuration");
            logger.debug("Terms acceptance saved", null);
        } catch (IOException e) {
            logger.error("Error saving terms acceptance", e);
        }
    }
}