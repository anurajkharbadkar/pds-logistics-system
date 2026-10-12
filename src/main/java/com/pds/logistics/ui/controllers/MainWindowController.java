package com.pds.logistics.ui.controllers;

import com.pds.logistics.config.DatabaseConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the main application window.
 * Handles sidebar navigation and content-pane swapping.
 */
public class MainWindowController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

    @FXML private StackPane  contentStack;
    @FXML private Label      lblDbStatus;
    @FXML private Label      lblStatus;
    @FXML private Label      lblStatusRight;
    @FXML private ProgressBar progressBar;

    // Nav buttons
    @FXML private Button btnDashboard, btnLocations, btnInventory, btnDemand;
    @FXML private Button btnRoutes, btnOptimize, btnNetwork, btnScenarios, btnHistory;

    private final Map<String, Node> pageCache = new HashMap<>();
    private Button currentNavButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        checkDbConnection();
        showDashboard();
    }

    // ─── Navigation Handlers ─────────────────────────────────────────────────

    @FXML public void showDashboard()   { navigateTo("Dashboard",   "/fxml/Dashboard.fxml",   btnDashboard); }
    @FXML public void showLocations()   { navigateTo("Locations",   "/fxml/Locations.fxml",   btnLocations); }
    @FXML public void showInventory()   { navigateTo("Inventory",   "/fxml/Inventory.fxml",   btnInventory); }
    @FXML public void showDemand()      { navigateTo("Demand",      "/fxml/Demand.fxml",      btnDemand); }
    @FXML public void showRoutes()      { navigateTo("Routes",      "/fxml/Routes.fxml",      btnRoutes); }
    @FXML public void showOptimize()    { navigateTo("Optimize",    "/fxml/Optimize.fxml",    btnOptimize); }
    @FXML public void showNetwork()     { navigateTo("Network",     "/fxml/Network.fxml",     btnNetwork); }
    @FXML public void showScenarios()   { navigateTo("Scenarios",   "/fxml/Scenarios.fxml",   btnScenarios); }
    @FXML public void showHistory()     { navigateTo("History",     "/fxml/History.fxml",     btnHistory); }

    private void navigateTo(String key, String fxmlPath, Button navBtn) {
        try {
            // Update active button style
            if (currentNavButton != null) {
                currentNavButton.getStyleClass().remove("active");
            }
            navBtn.getStyleClass().add("active");
            currentNavButton = navBtn;

            // Load or retrieve cached page
            Node page = pageCache.computeIfAbsent(key, k -> loadFxml(fxmlPath));
            if (page != null) {
                contentStack.getChildren().setAll(page);
                setStatus("Viewing: " + key);
            }
        } catch (Exception e) {
            log.error("Navigation error to {}: {}", key, e.getMessage(), e);
            showErrorAlert("Navigation Error", "Could not load view: " + fxmlPath);
        }
    }

    private Node loadFxml(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            return loader.load();
        } catch (IOException e) {
            log.error("Failed to load FXML {}: {}", path, e.getMessage(), e);
            return createErrorPlaceholder(path);
        }
    }

    private Node createErrorPlaceholder(String path) {
        Label lbl = new Label("⚠ Could not load: " + path);
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px;");
        return lbl;
    }

    // ─── DB Connection Check ─────────────────────────────────────────────────

    private void checkDbConnection() {
        Thread.ofVirtual().start(() -> {
            boolean ok = DatabaseConfig.testConnection();
            Platform.runLater(() -> {
                if (ok) {
                    lblDbStatus.setText("● Database Connected");
                    lblDbStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
                } else {
                    lblDbStatus.setText("● Database Offline");
                    lblDbStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                }
            });
        });
    }

    // ─── Status helpers ──────────────────────────────────────────────────────

    public void setStatus(String msg) {
        lblStatus.setText(msg);
    }

    public void setProgress(boolean visible) {
        progressBar.setVisible(visible);
    }

    public void setStatusRight(String msg) {
        lblStatusRight.setText(msg);
    }

    private void showErrorAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}
