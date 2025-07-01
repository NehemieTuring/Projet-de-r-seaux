package com.smartcrawler.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrawler.model.IndexPayload;
import com.smartcrawler.core.SchedulerService;
import com.smartcrawler.utils.FileUtils;
import com.smartcrawler.utils.Logger;
import com.smartcrawler.utils.LoggerFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import static com.smartcrawler.utils.ValidationUtils.isValidUrl;

/**
 * Contrôleur pour l'interface principale JavaFX avec un style moderne et professionnel.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class.getName());
    private UIManager uiManager;
    private ObservableList<String> urlList = FXCollections.observableArrayList();
    private ObservableList<CrawlReport> reportList = FXCollections.observableArrayList();

    @FXML
    private VBox rootContainer;
    @FXML
    private TextField urlField;
    @FXML
    private ListView<String> urlListView;
    @FXML
    private TableView<CrawlReport> historyTable;
    @FXML
    private TableColumn<CrawlReport, String> urlColumn;
    @FXML
    private TableColumn<CrawlReport, String> statusColumn;
    @FXML
    private TableColumn<CrawlReport, String> timestampColumn;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button addUrlButton;
    @FXML
    private Button removeUrlButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private void initialize() {
        setupUI();
        setupTableColumns();
        loadUserConfig();
        loadHistory();
        applyAnimations();
    }

    private void setupUI() {
        urlListView.setItems(urlList);
        urlListView.getStyleClass().add("modern-list-view");

        Label emptyLabel = new Label("Aucune URL ajoutée");
        emptyLabel.getStyleClass().add("empty-placeholder");
        urlListView.setPlaceholder(emptyLabel);

        historyTable.setItems(reportList);
        historyTable.getStyleClass().add("modern-table-view");

        Label emptyTableLabel = new Label("Aucun historique de crawling");
        emptyTableLabel.getStyleClass().add("empty-placeholder");
        historyTable.setPlaceholder(emptyTableLabel);

        urlField.setPromptText("Entrez une URL à crawler (ex: https://example.com)");
        urlField.getStyleClass().add("modern-text-field");

        setupButtons();

        statusLabel.setText("Service arrêté");
        statusLabel.getStyleClass().add("status-label");

        progressIndicator.setVisible(false);
        progressIndicator.getStyleClass().add("modern-progress");

        applyCSSStyles();
    }

    private void setupButtons() {
        addUrlButton.getStyleClass().addAll("modern-button", "primary-button");
        addUrlButton.setOnAction(e -> addUrl());

        removeUrlButton.getStyleClass().addAll("modern-button", "secondary-button");
        removeUrlButton.setOnAction(e -> removeUrl());

        startButton.getStyleClass().addAll("modern-button", "success-button");
        startButton.setOnAction(e -> startService());

        stopButton.getStyleClass().addAll("modern-button", "danger-button");
        stopButton.setOnAction(e -> stopService());

        removeUrlButton.setDisable(true);
        urlListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> removeUrlButton.setDisable(newVal == null)
        );
    }

    private void setupTableColumns() {
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        statusColumn.setCellFactory(column -> new TableCell<CrawlReport, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-success", "status-error", "status-pending");
                } else {
                    setText(status);
                    getStyleClass().removeAll("status-success", "status-error", "status-pending");

                    switch (status.toLowerCase()) {
                        case "success":
                        case "completed":
                            getStyleClass().add("status-success");
                            break;
                        case "error":
                        case "failed":
                            getStyleClass().add("status-error");
                            break;
                        default:
                            getStyleClass().add("status-pending");
                            break;
                    }
                }
            }
        });

        urlColumn.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.5));
        statusColumn.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.25));
        timestampColumn.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.25));
    }

    private void applyCSSStyles() {
        rootContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            }
        });
    }

    private void applyAnimations() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void animateButton(Button button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        scaleUp.setOnFinished(e -> scaleDown.play());
        scaleUp.play();
    }

    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
        uiManager.setMainController(this); // Initialiser la référence dans UIManager
        updateServiceStatus();
    }

    @FXML
    private void showProfile() {
        uiManager.showProfileInterface();
    }

    @FXML
    private void addUrl() {
        String url = urlField.getText().trim();
        if (!url.isEmpty()) {
            // Validation plus robuste
            if (!isValidUrl(url)) {
                showAlert("Erreur", "L'URL n'est pas valide. Elle doit être complète et commencer par http:// ou https://");
                return;
            }
            SchedulerService scheduler = uiManager.getSchedulerService();
            logger.info("Attempting to schedule crawl for URL: " + url, null);
            if (uiManager.getServiceState() != UIManager.ServiceState.RUNNING) {
                logger.warn("Services are not running, URL will be scheduled but not crawled immediately: " + url, null);
                showAlert("Information", "Les services ne sont pas démarrés. L'URL sera crawlée après avoir démarré les services.");
            }
            if (scheduler.scheduleCrawl(url)) {
                urlList.add(url);
                saveUserConfig();
                urlField.clear();
                logger.info("URL successfully scheduled: " + url, null);
                animateButton(addUrlButton);
                showSuccessMessage("URL ajoutée avec succès !");
                if (uiManager.getServiceState() == UIManager.ServiceState.RUNNING) {
                    logger.info("Triggering immediate crawl for: " + url, null);
                    scheduler.triggerImmediateCrawl(url);
                }
            } else {
                logger.error("Failed to schedule URL: Queue is full or URL already scheduled", null);
                showAlert("Erreur", "Impossible d'ajouter l'URL : La file d'attente est pleine ou l'URL est déjà planifiée");
            }
        } else {
            showAlert("Erreur", "L'URL ne peut pas être vide");
        }
    }


    @FXML
    private void removeUrl() {
        String selectedUrl = urlListView.getSelectionModel().getSelectedItem();
        if (selectedUrl != null) {
            urlList.remove(selectedUrl);
            uiManager.getSchedulerService().removeUrl(selectedUrl); // Retirer l'URL du scheduler
            saveUserConfig();
            logger.info("URL removed: " + selectedUrl, null);
            animateButton(removeUrlButton);
            showSuccessMessage("URL supprimée avec succès !");
        }
    }

    @FXML
    private void startService() {
        progressIndicator.setVisible(true);
        try {
            uiManager.startServices();
            animateButton(startButton);
            showSuccessMessage("Service démarré avec succès !");
        } finally {
            progressIndicator.setVisible(false);
        }
    }

    @FXML
    private void stopService() {
        progressIndicator.setVisible(true);
        try {
            uiManager.stopServices(false);
            animateButton(stopButton);
            showSuccessMessage("Service arrêté avec succès !");
        } finally {
            progressIndicator.setVisible(false);
        }
    }

    @FXML
    private void showContextMenu(ContextMenuEvent event) {
        String selectedUrl = urlListView.getSelectionModel().getSelectedItem();
        if (selectedUrl == null) {
            return;
        }

        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyItem = new MenuItem("Copier le lien");
        copyItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedUrl);
            clipboard.setContent(content);
            showSuccessMessage("Lien copié dans le presse-papiers !");
            logger.info("URL copied: " + selectedUrl, null);
        });

        MenuItem editItem = new MenuItem("Modifier le lien");
        editItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(selectedUrl);
            dialog.setTitle("Modifier l'URL");
            dialog.setHeaderText("Entrez la nouvelle URL");
            dialog.setContentText("URL :");
            dialog.showAndWait().ifPresent(newUrl -> {
                if (!newUrl.isEmpty() && newUrl.matches("^(https?://).+")) {
                    int index = urlList.indexOf(selectedUrl);
                    urlList.set(index, newUrl);
                    uiManager.getSchedulerService().updateUrl(selectedUrl, newUrl); // Mettre à jour l'URL dans le scheduler
                    saveUserConfig();
                    logger.info("URL modified from " + selectedUrl + " to " + newUrl, null);
                    showSuccessMessage("URL modifiée avec succès !");
                } else {
                    showAlert("Erreur", "L'URL doit être valide (http:// ou https://)");
                }
            });
        });

        MenuItem deleteItem = new MenuItem("Supprimer le lien");
        deleteItem.setOnAction(e -> removeUrl());

        MenuItem refreshItem = new MenuItem("Rafraîchir le crawl");
        refreshItem.setOnAction(e -> {
            if (uiManager.getServiceState() == UIManager.ServiceState.RUNNING) {
                uiManager.getSchedulerService().triggerImmediateCrawl(selectedUrl);
                logger.info("Crawl refreshed for: " + selectedUrl, null);
                showSuccessMessage("Crawl rafraîchi pour " + selectedUrl);
            } else {
                showAlert("Erreur", "Les services doivent être démarrés pour rafraîchir le crawl");
            }
        });

        contextMenu.getItems().addAll(copyItem, editItem, deleteItem, refreshItem);
        contextMenu.show(urlListView, event.getScreenX(), event.getScreenY());
    }

    public void updateServiceStatus() {
        UIManager.ServiceState state = uiManager.getServiceState();
        switch (state) {
            case RUNNING:
                statusLabel.setText("Service en cours d'exécution");
                statusLabel.setStyle("-fx-text-fill: #198754;");
                startButton.setDisable(true);
                stopButton.setDisable(false);
                progressIndicator.setVisible(false);
                break;
            case STOPPED:
                statusLabel.setText("Service arrêté");
                statusLabel.setStyle("-fx-text-fill: #dc3545;");
                startButton.setDisable(false);
                stopButton.setDisable(true);
                progressIndicator.setVisible(false);
                break;
            case ERROR:
                statusLabel.setText("Erreur dans les services");
                statusLabel.setStyle("-fx-text-fill: #ff9800;");
                startButton.setDisable(false);
                stopButton.setDisable(true);
                progressIndicator.setVisible(false);
                break;
        }
    }

    private void loadUserConfig() {
        Properties userConfig = new Properties();
        try {
            userConfig.load(Files.newInputStream(Paths.get("user-config.properties")));
            String urls = userConfig.getProperty("base.urls", "");
            if (!urls.isEmpty()) {
                urlList.addAll(List.of(urls.split(",")));
            }
        } catch (IOException e) {
            logger.debug("No user-config.properties found for URLs", null);
        }
    }

    private void saveUserConfig() {
        Properties userConfig = new Properties();
        userConfig.setProperty("base.urls", String.join(",", urlList));
        userConfig.setProperty("terms.accepted", "true");
        try {
            userConfig.store(Files.newOutputStream(Paths.get("user-config.properties")), "User configuration");
            logger.debug("User config saved", null);
        } catch (IOException e) {
            logger.error("Error saving user config", e);
        }
    }

    private void loadHistory() {
        String reportFile = "reports/crawl-report.json";
        Path path = Paths.get(reportFile);

        try {
            // Créer le dossier s'il n'existe pas
            Files.createDirectories(path.getParent());

            // Créer un fichier vide JSON valide s'il n'existe pas
            if (Files.notExists(path)) {
                Files.writeString(path, "[]"); // Liste vide JSON
                logger.info("No crawl report file found. Created empty report file.", null);
                return;
            }

            String content = FileUtils.readFromFile(reportFile).trim();
            if (content.isEmpty() || content.equals("[]")) {
                logger.info("Crawl report file is empty. Nothing to load.", null);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<IndexPayload> payloads = mapper.readValue(
                    content,
                    mapper.getTypeFactory().constructCollectionType(List.class, IndexPayload.class)
            );

            reportList.clear();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
            payloads.forEach(payload ->
                    payload.getResults().forEach(result ->
                            reportList.add(new CrawlReport(
                                    result.getUrl(),
                                    result.getStatus().toString(),
                                    formatter.format(Instant.ofEpochMilli(payload.getTimestamp()))
                            ))
                    )
            );

            logger.info("Crawl history loaded, " + reportList.size() + " entries", null);

        } catch (IOException e) {
            logger.error("Error loading crawl history", e);
            showAlert("Erreur", "Impossible de charger l'historique des crawls");
        }
    }

    public void updateHistory(IndexPayload payload) {
        Platform.runLater(() -> {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
            payload.getResults().forEach(result ->
                    reportList.add(new CrawlReport(
                            result.getUrl(),
                            result.getStatus().toString(),
                            formatter.format(Instant.ofEpochMilli(payload.getTimestamp()))
                    ))
            );
            logger.info("History updated with new crawl results", null);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        alert.showAndWait();
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/dialog.css").toExternalForm());
        alert.showAndWait();
    }

    /**
     * Modèle pour les entrées du tableau d'historique.
     */
    public static class CrawlReport {
        private final String url;
        private final String status;
        private final String timestamp;

        public CrawlReport(String url, String status, String timestamp) {
            this.url = url;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getUrl() {
            return url;
        }

        public String getStatus() {
            return status;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}