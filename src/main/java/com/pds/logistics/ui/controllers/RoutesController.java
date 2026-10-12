package com.pds.logistics.ui.controllers;

import com.pds.logistics.dao.LocationDAO;
import com.pds.logistics.dao.RouteDAO;
import com.pds.logistics.model.Location;
import com.pds.logistics.model.TransportRoute;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for route configuration and management.
 */
public class RoutesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(RoutesController.class);

    @FXML private TableView<TransportRoute> tblRoutes;
    @FXML private TableColumn<TransportRoute, Integer> colId;
    @FXML private TableColumn<TransportRoute, String> colSource, colDest, colCapacity, colCost, colDistance, colMode, colActive, colRemarks;

    // Form fields
    @FXML private ComboBox<Location> cmbSource;
    @FXML private ComboBox<Location> cmbDest;
    @FXML private TextField txfCapacity;
    @FXML private TextField txfCost;
    @FXML private TextField txfDistance;
    @FXML private ComboBox<String> cmbMode;
    @FXML private CheckBox chkActive;
    @FXML private TextField txfRemarks;
    @FXML private Label lblFormStatus;
    @FXML private Button btnSave, btnDelete, btnClear;

    private final RouteDAO routeDAO = new RouteDAO();
    private final LocationDAO locationDAO = new LocationDAO();

    private final ObservableList<TransportRoute> routeData = FXCollections.observableArrayList();
    private final ObservableList<Location> locationList = FXCollections.observableArrayList();
    private TransportRoute selectedRoute;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupLocationComboBoxes();

        cmbMode.setItems(FXCollections.observableArrayList("ROAD", "RAIL", "WATERWAY"));
        cmbMode.getSelectionModel().selectFirst();
        chkActive.setSelected(true);

        tblRoutes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            }
        });

        loadLocations();
        loadRoutes();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSource.setCellValueFactory(r -> new SimpleStringProperty(
                r.getValue().getSource() != null ? r.getValue().getSource().getName() + " (" + r.getValue().getSource().getCode() + ")" : ""));
        colDest.setCellValueFactory(r -> new SimpleStringProperty(
                r.getValue().getDestination() != null ? r.getValue().getDestination().getName() + " (" + r.getValue().getDestination().getCode() + ")" : ""));
        colCapacity.setCellValueFactory(r -> new SimpleStringProperty(String.format("%.1f MT", r.getValue().getCapacityMT())));
        colCost.setCellValueFactory(r -> new SimpleStringProperty(String.format("₹%.2f", r.getValue().getUnitCostPerMT())));
        colDistance.setCellValueFactory(r -> new SimpleStringProperty(String.format("%.1f km", r.getValue().getDistanceKm())));
        colMode.setCellValueFactory(new PropertyValueFactory<>("transportMode"));
        colActive.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().isActive() ? "Active" : "Inactive"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        tblRoutes.setItems(routeData);
    }

    private void setupLocationComboBoxes() {
        StringConverter<Location> converter = new StringConverter<>() {
            @Override
            public String toString(Location loc) {
                return loc == null ? "" : loc.getName() + " [" + loc.getCode() + "] (" + loc.getType() + ")";
            }

            @Override
            public Location fromString(String string) {
                return null;
            }
        };

        cmbSource.setConverter(converter);
        cmbDest.setConverter(converter);
        cmbSource.setItems(locationList);
        cmbDest.setItems(locationList);
    }

    private void loadLocations() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Location> locs = locationDAO.findAll();
                Platform.runLater(() -> {
                    locationList.setAll(locs);
                });
            } catch (Exception e) {
                log.error("Failed to load locations for routes: {}", e.getMessage(), e);
            }
        });
    }

    private void loadRoutes() {
        Thread.ofVirtual().start(() -> {
            try {
                List<TransportRoute> routes = routeDAO.findAll();
                Platform.runLater(() -> {
                    routeData.setAll(routes);
                    lblFormStatus.setText("Loaded " + routes.size() + " routes.");
                });
            } catch (Exception e) {
                log.error("Failed to load routes: {}", e.getMessage(), e);
                Platform.runLater(() -> lblFormStatus.setText("Error loading routes: " + e.getMessage()));
            }
        });
    }

    private void populateForm(TransportRoute r) {
        selectedRoute = r;
        if (r.getSource() != null) {
            for (Location l : locationList) {
                if (l.getId() == r.getSource().getId()) {
                    cmbSource.setValue(l);
                    break;
                }
            }
        }
        if (r.getDestination() != null) {
            for (Location l : locationList) {
                if (l.getId() == r.getDestination().getId()) {
                    cmbDest.setValue(l);
                    break;
                }
            }
        }
        txfCapacity.setText(String.valueOf(r.getCapacityMT()));
        txfCost.setText(String.valueOf(r.getUnitCostPerMT()));
        txfDistance.setText(String.valueOf(r.getDistanceKm()));
        cmbMode.setValue(r.getTransportMode());
        chkActive.setSelected(r.isActive());
        txfRemarks.setText(r.getRemarks() != null ? r.getRemarks() : "");
        lblFormStatus.setText("Editing route #" + r.getId());
        btnDelete.setDisable(false);
    }

    @FXML
    public void onSave() {
        Location src = cmbSource.getValue();
        Location dst = cmbDest.getValue();
        if (src == null || dst == null) {
            lblFormStatus.setText("Please select source and destination locations.");
            return;
        }
        if (src.getId() == dst.getId()) {
            lblFormStatus.setText("Source and destination cannot be identical.");
            return;
        }

        double cap, cost, dist;
        try {
            cap = Double.parseDouble(txfCapacity.getText().trim());
            cost = Double.parseDouble(txfCost.getText().trim());
            dist = Double.parseDouble(txfDistance.getText().trim());
            if (cap < 0 || cost < 0 || dist < 0) throw new IllegalArgumentException("Values must be non-negative");
        } catch (Exception ex) {
            lblFormStatus.setText("Invalid numeric input: " + ex.getMessage());
            return;
        }

        TransportRoute route = selectedRoute != null ? selectedRoute : new TransportRoute();
        route.setSource(src);
        route.setDestination(dst);
        route.setCapacityMT(cap);
        route.setUnitCostPerMT(cost);
        route.setDistanceKm(dist);
        route.setTransportMode(cmbMode.getValue() != null ? cmbMode.getValue() : "ROAD");
        route.setActive(chkActive.isSelected());
        route.setRemarks(txfRemarks.getText().trim());

        Thread.ofVirtual().start(() -> {
            try {
                if (route.getId() == 0) {
                    routeDAO.insert(route);
                } else {
                    routeDAO.update(route);
                }
                Platform.runLater(() -> {
                    lblFormStatus.setText("Saved route successfully.");
                    onClear();
                    loadRoutes();
                });
            } catch (Exception e) {
                log.error("Failed to save route: {}", e.getMessage(), e);
                Platform.runLater(() -> lblFormStatus.setText("Error saving: " + e.getMessage()));
            }
        });
    }

    @FXML
    public void onDelete() {
        if (selectedRoute == null) return;
        int id = selectedRoute.getId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete route #" + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Thread.ofVirtual().start(() -> {
                    try {
                        routeDAO.delete(id);
                        Platform.runLater(() -> {
                            lblFormStatus.setText("Deleted route #" + id);
                            onClear();
                            loadRoutes();
                        });
                    } catch (Exception e) {
                        log.error("Failed to delete route: {}", e.getMessage(), e);
                        Platform.runLater(() -> lblFormStatus.setText("Delete error: " + e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    public void onClear() {
        selectedRoute = null;
        tblRoutes.getSelectionModel().clearSelection();
        cmbSource.setValue(null);
        cmbDest.setValue(null);
        txfCapacity.clear();
        txfCost.clear();
        txfDistance.clear();
        cmbMode.getSelectionModel().selectFirst();
        chkActive.setSelected(true);
        txfRemarks.clear();
        lblFormStatus.setText("Ready");
        btnDelete.setDisable(true);
    }
}
