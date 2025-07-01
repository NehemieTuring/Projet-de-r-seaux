package com.smartcrawler.utils;

import com.smartcrawler.ui.UIManager;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Gestionnaire de l'icône dans la barre des tâches.
 */
public class SystemTrayManager {

    private static final Logger logger = LoggerFactory.getLogger(SystemTrayManager.class.getName());
    private final UIManager uiManager;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private MenuItem startItem;
    private MenuItem stopItem;

    public SystemTrayManager(UIManager uiManager) {
        this.uiManager = uiManager;
    }

    /**
     * Initialise l'icône dans la barre des tâches.
     */
    public void initSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("System tray not supported", null);
            return;
        }

        Platform.runLater(() -> {
            try {
                systemTray = SystemTray.getSystemTray();
                // Charger l'icône, avec un fallback si introuvable
                Image image = null;
                try {
                    image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/com/smartcrawler/smartcrawleragent/images/ico.png"));
                } catch (Exception e) {
                    logger.warn("Could not load tray icon, using default", e);
                    image = Toolkit.getDefaultToolkit().createImage(new byte[0]); // Icône vide par défaut
                }
                trayIcon = new TrayIcon(image, "Smart Crawler Agent");
                trayIcon.setImageAutoSize(true);

                PopupMenu popup = new PopupMenu();
                MenuItem openItem = new MenuItem("Open");
                startItem = new MenuItem("Start Service");
                stopItem = new MenuItem("Stop Service");
                MenuItem exitItem = new MenuItem("Exit");

                openItem.addActionListener(e -> Platform.runLater(() -> {
                    logger.info("System tray: Open action triggered", null);
                    Stage stage = uiManager.getPrimaryStage();
                    if (stage != null) {
                        stage.show();
                        stage.toFront();
                    } else {
                        logger.warn("Primary stage is null, cannot open window", null);
                    }
                }));

                startItem.addActionListener(e -> Platform.runLater(() -> {
                    logger.info("System tray: Start Service action triggered", null);
                    uiManager.startServices();
                    updateMenuState();
                }));

                stopItem.addActionListener(e -> Platform.runLater(() -> {
                    logger.info("System tray: Stop Service action triggered", null);
                    uiManager.stopServices(false); // Ne pas quitter l'application
                    updateMenuState();
                }));

                exitItem.addActionListener(e -> Platform.runLater(() -> {
                    logger.info("System tray: Exit action triggered", null);
                    uiManager.stopServices(true); // Quitter l'application
                }));

                popup.add(openItem);
                popup.add(startItem);
                popup.add(stopItem);
                popup.addSeparator();
                popup.add(exitItem);

                trayIcon.setPopupMenu(popup);
                systemTray.add(trayIcon);
                updateMenuState(); // Initialiser l'état des boutons
                logger.info("System tray initialized", null);
            } catch (AWTException e) {
                logger.error("Error initializing system tray", e);
            }
        });
    }

    /**
     * Met à jour l'état des éléments du menu en fonction de l'état des services.
     */
    private void updateMenuState() {
        UIManager.ServiceState state = uiManager.getServiceState();
        startItem.setEnabled(state == UIManager.ServiceState.STOPPED || state == UIManager.ServiceState.ERROR);
        stopItem.setEnabled(state == UIManager.ServiceState.RUNNING);
    }

    /**
     * Supprime l'icône de la barre des tâches.
     */
    public void removeSystemTray() {
        if (systemTray != null && trayIcon != null) {
            Platform.runLater(() -> {
                systemTray.remove(trayIcon);
                logger.info("System tray removed", null);
                trayIcon = null;
                systemTray = null;
            });
        }
    }
}