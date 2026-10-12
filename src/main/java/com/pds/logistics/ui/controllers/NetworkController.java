package com.pds.logistics.ui.controllers;

import com.pds.logistics.dao.*;
import com.pds.logistics.model.*;
import com.pds.logistics.service.*;
import com.pds.logistics.ui.components.NetworkCanvas;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 * Controller for the Network Graph visualization view.
 */
public class NetworkController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(NetworkController.class);

    @FXML private StackPane         canvasContainer;
    @FXML private ComboBox<String>  cmbGrainType;
    @FXML private CheckBox          chkShowFlow;
    @FXML private Label             lblLegend;
    @FXML private Label             lblNodeCount;
    @FXML private Label             lblEdgeCount;
    @FXML private Label             lblFlowEdges;

    private NetworkCanvas           canvas;
    private final LocationDAO       locationDAO  = new LocationDAO();
    private final RouteDAO          routeDAO     = new RouteDAO();
    private final OptimizationDAO   optDAO       = new OptimizationDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbGrainType.setItems(FXCollections.observableArrayList("WHEAT", "RICE", "None"));
        cmbGrainType.getSelectionModel().select("WHEAT");
        chkShowFlow.setSelected(true);

        // Canvas fills the StackPane
        canvas = new NetworkCanvas(800, 500);
        canvasContainer.getChildren().add(canvas);

        // Bind canvas size to container size
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        loadAndDraw();
    }

    @FXML
    public void refreshGraph() {
        loadAndDraw();
    }

    private void loadAndDraw() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Location>       locs       = locationDAO.findAllActive();
                List<TransportRoute> routes     = routeDAO.findAll();
                List<AllocationResult> allocs   = new ArrayList<>();

                String grain = cmbGrainType.getValue();
                if (chkShowFlow.isSelected() && !"None".equals(grain)) {
                    // Load most recent run's allocations
                    var history = optDAO.findAllRuns();
                    for (var run : history) {
                        if (run.getGrainType().name().equals(grain)) {
                            OptimizationResult full = optDAO.findById(run.getId());
                            if (full != null) { allocs = full.getAllocations(); break; }
                        }
                    }
                }

                final List<AllocationResult> finalAllocs = allocs;
                final int nodeCount = locs.size(), edgeCount = routes.size();
                final int flowEdges = (int) finalAllocs.stream()
                        .filter(a -> a.getAllocatedMT() > 0.001).count();

                Platform.runLater(() -> {
                    canvas.setData(locs, routes, finalAllocs);
                    lblNodeCount.setText(String.valueOf(nodeCount));
                    lblEdgeCount.setText(String.valueOf(edgeCount));
                    lblFlowEdges.setText(String.valueOf(flowEdges));
                });

            } catch (Exception e) {
                log.error("Network graph load error: {}", e.getMessage(), e);
                Platform.runLater(() ->
                        canvas.setData(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
            }
        });
    }
}
