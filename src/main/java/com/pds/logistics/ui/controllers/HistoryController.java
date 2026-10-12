package com.pds.logistics.ui.controllers;

import com.pds.logistics.dao.OptimizationDAO;
import com.pds.logistics.model.*;
import com.pds.logistics.service.CsvExportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * History view controller – shows optimization run history with export.
 */
public class HistoryController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @FXML private TableView<OptimizationResult>            tblHistory;
    @FXML private TableColumn<OptimizationResult, String>  colRunId, colScenario, colGrain, colTimestamp;
    @FXML private TableColumn<OptimizationResult, String>  colTransported, colCost, colEfficiency, colStatus;
    @FXML private Label lblRowCount;
    @FXML private Button btnExportHistory, btnExportSelected;

    private final ObservableList<OptimizationResult> data = FXCollections.observableArrayList();
    private final OptimizationDAO dao        = new OptimizationDAO();
    private final CsvExportService csvExport = new CsvExportService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colRunId.setCellValueFactory(c       -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colScenario.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getScenarioName()));
        colGrain.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getGrainType().getDisplayName()));
        colTimestamp.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().getRunTimestamp() != null ? c.getValue().getRunTimestamp().format(DT) : "–"));
        colTransported.setCellValueFactory(c -> new SimpleStringProperty("%.1f MT".formatted(c.getValue().getTotalTransportedMT())));
        colCost.setCellValueFactory(c        -> new SimpleStringProperty("₹%,.0f".formatted(c.getValue().getTotalCost())));
        colEfficiency.setCellValueFactory(c  -> new SimpleStringProperty("%.1f%%".formatted(c.getValue().getNetworkEfficiencyPct())));
        colStatus.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getStatus()));
        tblHistory.setItems(data);
    }

    private void loadData() {
        Thread.ofVirtual().start(() -> {
            try {
                var list = dao.findAllRuns();
                Platform.runLater(() -> {
                    data.setAll(list);
                    lblRowCount.setText("Total runs: " + list.size());
                });
            } catch (Exception e) {
                log.error("History load error: {}", e.getMessage(), e);
            }
        });
    }

    @FXML public void refreshHistory() { loadData(); }

    @FXML public void exportHistory() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export History");
        fc.setInitialFileName("pds_history.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(tblHistory.getScene().getWindow());
        if (f != null) {
            try {
                csvExport.exportHistory(data, f.getAbsolutePath());
                showAlert("History exported to " + f.getAbsolutePath());
            } catch (Exception e) { showAlert("Export failed: " + e.getMessage()); }
        }
    }

    @FXML public void exportSelected() {
        OptimizationResult sel = tblHistory.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a run first."); return; }

        Thread.ofVirtual().start(() -> {
            try {
                OptimizationResult full = dao.findById(sel.getId());
                Platform.runLater(() -> {
                    FileChooser fc = new FileChooser();
                    fc.setInitialFileName("pds_run_" + sel.getId() + ".csv");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
                    File f = fc.showSaveDialog(tblHistory.getScene().getWindow());
                    if (f != null && full != null) {
                        try {
                            csvExport.exportResult(full, f.getAbsolutePath());
                            showAlert("Exported run #" + sel.getId());
                        } catch (Exception e) { showAlert("Export failed: " + e.getMessage()); }
                    }
                });
            } catch (Exception e) { log.error("Export error: {}", e.getMessage(), e); }
        });
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
