package com.pds.logistics.ui.controllers;

import com.pds.logistics.algorithm.Scenario;
import com.pds.logistics.model.*;
import com.pds.logistics.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the Run Optimization view.
 * Triggers MCMF solve and displays tabular results.
 */
public class OptimizeController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(OptimizeController.class);
    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    // ── Config controls ───────────────────────────────────────────────────────
    @FXML private ComboBox<String>  cmbGrainType;
    @FXML private CheckBox          chkPersist;

    // ── Summary labels ────────────────────────────────────────────────────────
    @FXML private Label lblStatus;
    @FXML private Label lblTransported;
    @FXML private Label lblTotalCost;
    @FXML private Label lblFulfilled;
    @FXML private Label lblUnmet;
    @FXML private Label lblEfficiency;
    @FXML private Label lblDuration;
    @FXML private Label lblAlgorithm;
    @FXML private ProgressBar pbEfficiency;

    // ── Results table ─────────────────────────────────────────────────────────
    @FXML private TableView<AllocationResult>           tblResults;
    @FXML private TableColumn<AllocationResult, String> colSrc;
    @FXML private TableColumn<AllocationResult, String> colDst;
    @FXML private TableColumn<AllocationResult, String> colMode;
    @FXML private TableColumn<AllocationResult, String> colCapacity;
    @FXML private TableColumn<AllocationResult, String> colAllocated;
    @FXML private TableColumn<AllocationResult, String> colUnitCost;
    @FXML private TableColumn<AllocationResult, String> colCostTotal;

    // ── Action buttons ────────────────────────────────────────────────────────
    @FXML private Button btnRun;
    @FXML private Button btnExport;
    @FXML private ProgressIndicator spinner;

    private final OptimizationService service   = new OptimizationService();
    private final CsvExportService    csvExport = new CsvExportService();

    private OptimizationResult lastResult;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbGrainType.setItems(FXCollections.observableArrayList("WHEAT", "RICE"));
        cmbGrainType.getSelectionModel().selectFirst();
        chkPersist.setSelected(true);

        setupTable();
        spinner.setVisible(false);
        btnExport.setDisable(true);
    }

    private void setupTable() {
        colSrc.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRoute().getSource().getName()));
        colDst.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRoute().getDestination().getName()));
        colMode.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRoute().getTransportMode()));
        colCapacity.setCellValueFactory(c ->
                new SimpleStringProperty("%.0f MT".formatted(c.getValue().getRoute().getCapacityMT())));
        colAllocated.setCellValueFactory(c ->
                new SimpleStringProperty("%.2f MT".formatted(c.getValue().getAllocatedMT())));
        colUnitCost.setCellValueFactory(c ->
                new SimpleStringProperty("₹%.2f".formatted(c.getValue().getRoute().getUnitCostPerMT())));
        colCostTotal.setCellValueFactory(c ->
                new SimpleStringProperty("₹%,.0f".formatted(c.getValue().getCostContribution())));

        tblResults.setPlaceholder(new Label("Run optimization to see route allocations"));
    }

    @FXML
    public void runOptimization() {
        String grainStr = cmbGrainType.getValue();
        if (grainStr == null) {
            showAlert("Select a grain type first.");
            return;
        }

        GrainType grain = GrainType.valueOf(grainStr);
        boolean persist = chkPersist.isSelected();

        setRunning(true);
        lblStatus.setText("Running MCMF optimizer…");

        Thread.ofVirtual().start(() -> {
            try {
                Scenario baseline = ScenarioFactory.createBaseline();
                OptimizationResult result = service.runOptimization(grain, baseline, persist);
                Platform.runLater(() -> displayResult(result));
            } catch (Exception e) {
                log.error("Optimization failed: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    setRunning(false);
                    lblStatus.setText("❌ Error: " + e.getMessage());
                });
            }
        });
    }

    private void displayResult(OptimizationResult result) {
        lastResult = result;
        setRunning(false);

        // Status chip
        String statusText = switch (result.getStatus()) {
            case "SUCCESS"    -> "✅ Optimal Solution Found";
            case "PARTIAL"    -> "⚠ Partial Solution (Supply Shortage)";
            case "INFEASIBLE" -> "❌ Infeasible – No Flow Possible";
            default           -> result.getStatus();
        };
        lblStatus.setText(statusText);

        lblTransported.setText("%.2f MT".formatted(result.getTotalTransportedMT()));
        lblTotalCost.setText("₹%,.0f".formatted(result.getTotalCost()));
        lblFulfilled.setText("%.2f MT".formatted(result.getFulfilledDemandMT()));
        lblUnmet.setText("%.2f MT".formatted(result.getUnmetDemandMT()));
        lblEfficiency.setText("%.1f%%".formatted(result.getNetworkEfficiencyPct()));
        lblDuration.setText(result.getSolveDurationMs() + " ms");
        lblAlgorithm.setText(result.getAlgorithmUsed());
        pbEfficiency.setProgress(result.getNetworkEfficiencyPct() / 100.0);

        tblResults.setItems(FXCollections.observableArrayList(result.getAllocations()));
        btnExport.setDisable(false);
    }

    @FXML
    public void exportResult() {
        if (lastResult == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Optimization Result");
        fc.setInitialFileName("pds_result_%d.csv".formatted(lastResult.getId()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showSaveDialog(tblResults.getScene().getWindow());
        if (f != null) {
            try {
                csvExport.exportResult(lastResult, f.getAbsolutePath());
                showAlert("Exported successfully to: " + f.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Export failed: " + e.getMessage());
            }
        }
    }

    private void setRunning(boolean running) {
        btnRun.setDisable(running);
        spinner.setVisible(running);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
