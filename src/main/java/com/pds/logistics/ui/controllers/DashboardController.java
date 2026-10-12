package com.pds.logistics.ui.controllers;

import com.pds.logistics.service.OptimizationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Dashboard view controller – loads and displays live summary metrics.
 */
public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    // ── Metric labels ────────────────────────────────────────────────────────

    @FXML private Label lblTotalLocations;
    @FXML private Label lblActiveRoutes;
    @FXML private Label lblWheatSupply;
    @FXML private Label lblRiceSupply;
    @FXML private Label lblWheatDemand;
    @FXML private Label lblRiceDemand;
    @FXML private Label lblLastWheatCost;
    @FXML private Label lblLastWheatEff;
    @FXML private Label lblLastRiceCost;
    @FXML private Label lblLastRiceEff;
    @FXML private Label lblLastRunTime;
    @FXML private Label lblDbStatus;
    @FXML private ProgressBar pbWheatEff;
    @FXML private ProgressBar pbRiceEff;

    private final OptimizationService service = new OptimizationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadMetrics();
    }

    private void loadMetrics() {
        Thread.ofVirtual().start(() -> {
            try {
                int    locations  = service.countLocations();
                int    routes     = service.countActiveRoutes();
                double wSupply    = service.getTotalWheatSupply();
                double rSupply    = service.getTotalRiceSupply();
                double wDemand    = service.getTotalWheatDemand();
                double rDemand    = service.getTotalRiceDemand();

                var history = service.getOptimizationHistory();
                double lastWCost = 0, lastWEff = 0, lastRCost = 0, lastREff = 0;
                String lastRunTs = "No runs yet";

                if (!history.isEmpty()) {
                    var last = history.get(0);
                    lastRunTs = last.getRunTimestamp() != null
                            ? last.getRunTimestamp().toString().replace("T", " ") : "–";
                    for (var r : history) {
                        switch (r.getGrainType()) {
                            case WHEAT -> { lastWCost = r.getTotalCost(); lastWEff = r.getNetworkEfficiencyPct(); }
                            case RICE  -> { lastRCost = r.getTotalCost(); lastREff = r.getNetworkEfficiencyPct(); }
                            default    -> {}
                        }
                    }
                }

                final double fWEff = lastWEff, fREff = lastREff;
                final double fWCost = lastWCost, fRCost = lastRCost;
                final String fRunTs = lastRunTs;

                Platform.runLater(() -> {
                    lblTotalLocations.setText(String.valueOf(locations));
                    lblActiveRoutes.setText(String.valueOf(routes));
                    lblWheatSupply.setText("%.0f MT".formatted(wSupply));
                    lblRiceSupply.setText("%.0f MT".formatted(rSupply));
                    lblWheatDemand.setText("%.0f MT".formatted(wDemand));
                    lblRiceDemand.setText("%.0f MT".formatted(rDemand));
                    lblLastWheatCost.setText(fWCost > 0 ? "₹%,.0f".formatted(fWCost) : "–");
                    lblLastWheatEff.setText("%.1f%%".formatted(fWEff));
                    lblLastRiceCost.setText(fRCost > 0 ? "₹%,.0f".formatted(fRCost) : "–");
                    lblLastRiceEff.setText("%.1f%%".formatted(fREff));
                    lblLastRunTime.setText(fRunTs);
                    pbWheatEff.setProgress(fWEff / 100.0);
                    pbRiceEff.setProgress(fREff / 100.0);
                    lblDbStatus.setText("● Connected");
                    lblDbStatus.setStyle("-fx-text-fill: #22c55e;");
                });

            } catch (Exception e) {
                log.error("Dashboard load error: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    lblDbStatus.setText("● DB Error");
                    lblDbStatus.setStyle("-fx-text-fill: #ef4444;");
                });
            }
        });
    }

    @FXML
    public void refreshDashboard() {
        loadMetrics();
    }
}
