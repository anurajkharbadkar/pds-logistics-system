package com.pds.logistics.ui.controllers;

import com.pds.logistics.model.*;
import com.pds.logistics.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 * What-If Scenario controller.
 * Lets users select a scenario type, configure parameters, run the solver,
 * and compare the result against the baseline.
 */
public class ScenariosController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ScenariosController.class);

    @FXML private ComboBox<String>  cmbScenarioType;
    @FXML private ComboBox<String>  cmbGrainType;
    @FXML private Slider            sldrSupplyChange;
    @FXML private Slider            sldrDemandChange;
    @FXML private Label             lblSupplyPct;
    @FXML private Label             lblDemandPct;
    @FXML private TextField         txfRouteIds;
    @FXML private Button            btnRunScenario;
    @FXML private ProgressIndicator spinner;

    // Comparison labels
    @FXML private Label lblBaseTransported, lblBaseEfficiency, lblBaseCost;
    @FXML private Label lblScenTransported, lblScenEfficiency, lblScenCost;
    @FXML private Label lblDeltaTransported, lblDeltaEfficiency, lblDeltaCost;
    @FXML private Label lblScenStatus;

    private final OptimizationService service = new OptimizationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbScenarioType.setItems(FXCollections.observableArrayList(
                "Supply Shock", "Demand Surge", "Route Failure",
                "Cost Change (+20%)", "Capacity Reduction (-30%)"));
        cmbScenarioType.getSelectionModel().selectFirst();
        cmbGrainType.setItems(FXCollections.observableArrayList("WHEAT", "RICE"));
        cmbGrainType.getSelectionModel().selectFirst();

        sldrSupplyChange.valueProperty().addListener((obs, o, n) ->
                lblSupplyPct.setText("%.0f%%".formatted(n.doubleValue())));
        sldrDemandChange.valueProperty().addListener((obs, o, n) ->
                lblDemandPct.setText("%.0f%%".formatted(n.doubleValue())));

        spinner.setVisible(false);
    }

    @FXML
    public void runScenario() {
        String scenType = cmbScenarioType.getValue();
        GrainType grain = GrainType.valueOf(cmbGrainType.getValue());

        setRunning(true);

        Thread.ofVirtual().start(() -> {
            try {
                // 1. Run baseline
                com.pds.logistics.algorithm.Scenario baseline = ScenarioFactory.createBaseline();
                OptimizationResult baseResult = service.runOptimization(grain, baseline, false);

                // 2. Build scenario
                com.pds.logistics.algorithm.Scenario scenario = buildScenario(scenType);
                OptimizationResult scenResult = service.runOptimization(grain, scenario, false);

                Platform.runLater(() -> displayComparison(baseResult, scenResult, scenType));
            } catch (Exception e) {
                log.error("Scenario run error: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    setRunning(false);
                    lblScenStatus.setText("❌ Error: " + e.getMessage());
                });
            }
        });
    }

    private com.pds.logistics.algorithm.Scenario buildScenario(String type) {
        return switch (type) {
            case "Supply Shock"        -> ScenarioFactory.createSupplyShock(sldrSupplyChange.getValue());
            case "Demand Surge"        -> ScenarioFactory.createDemandSurge(sldrDemandChange.getValue());
            case "Route Failure"       -> {
                List<Integer> ids = parseRouteIds(txfRouteIds.getText());
                yield ScenarioFactory.createRouteFailure(ids);
            }
            case "Cost Change (+20%)"  -> {
                // 20% increase on all routes – pass empty map to use multiplier override
                yield ScenarioFactory.createCostChange(new HashMap<>());
            }
            case "Capacity Reduction (-30%)" -> ScenarioFactory.createCapacityChange(new HashMap<>());
            default -> ScenarioFactory.createBaseline();
        };
    }

    private void displayComparison(OptimizationResult base, OptimizationResult scen, String name) {
        setRunning(false);
        lblScenStatus.setText("✅ Scenario: " + name);

        lblBaseTransported.setText("%.1f MT".formatted(base.getTotalTransportedMT()));
        lblBaseEfficiency.setText("%.1f%%".formatted(base.getNetworkEfficiencyPct()));
        lblBaseCost.setText("₹%,.0f".formatted(base.getTotalCost()));

        lblScenTransported.setText("%.1f MT".formatted(scen.getTotalTransportedMT()));
        lblScenEfficiency.setText("%.1f%%".formatted(scen.getNetworkEfficiencyPct()));
        lblScenCost.setText("₹%,.0f".formatted(scen.getTotalCost()));

        double dtMT  = scen.getTotalTransportedMT()    - base.getTotalTransportedMT();
        double dEff  = scen.getNetworkEfficiencyPct()  - base.getNetworkEfficiencyPct();
        double dCost = scen.getTotalCost()             - base.getTotalCost();

        lblDeltaTransported.setText("%+.1f MT".formatted(dtMT));
        lblDeltaEfficiency.setText("%+.1f%%".formatted(dEff));
        lblDeltaCost.setText("%+,.0f ₹".formatted(dCost));

        String positiveStyle = "-fx-text-fill: #22c55e;";
        String negativeStyle = "-fx-text-fill: #ef4444;";
        lblDeltaTransported.setStyle(dtMT  >= 0 ? positiveStyle : negativeStyle);
        lblDeltaEfficiency.setStyle(dEff  >= 0 ? positiveStyle : negativeStyle);
        lblDeltaCost.setStyle(dCost <= 0 ? positiveStyle : negativeStyle);
    }

    private List<Integer> parseRouteIds(String text) {
        List<Integer> ids = new ArrayList<>();
        if (text == null || text.isBlank()) return ids;
        for (String part : text.split("[,\\s]+")) {
            try { ids.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
        }
        return ids;
    }

    private void setRunning(boolean r) {
        btnRunScenario.setDisable(r);
        spinner.setVisible(r);
    }
}
