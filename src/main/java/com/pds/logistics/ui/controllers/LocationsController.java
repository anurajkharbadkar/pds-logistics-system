package com.pds.logistics.ui.controllers;

import com.pds.logistics.dao.LocationDAO;
import com.pds.logistics.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * CRUD controller for Location management.
 */
public class LocationsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LocationsController.class);

    @FXML private TableView<Location>           tblLocations;
    @FXML private TableColumn<Location, Integer> colId;
    @FXML private TableColumn<Location, String>  colCode, colName, colType, colDistrict, colState, colActive;

    // Form fields
    @FXML private TextField txfCode, txfName, txfDistrict, txfState, txfLat, txfLon, txfCx, txfCy;
    @FXML private ComboBox<LocationType>         cmbType;
    @FXML private CheckBox chkActive;
    @FXML private Label    lblFormStatus;
    @FXML private Button   btnSave, btnDelete, btnClear;

    private final LocationDAO   dao  = new LocationDAO();
    private final ObservableList<Location> data = FXCollections.observableArrayList();
    private Location selectedLocation;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        cmbType.setItems(FXCollections.observableArrayList(
                LocationType.DEPOT, LocationType.WAREHOUSE,
                LocationType.DISTRIBUTION_CENTER, LocationType.FAIR_PRICE_SHOP));
        cmbType.getSelectionModel().selectFirst();
        chkActive.setSelected(true);

        tblLocations.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) populateForm(n);
        });

        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getDisplayName()));
        colDistrict.setCellValueFactory(new PropertyValueFactory<>("district"));
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "✓" : "✗"));
        tblLocations.setItems(data);
    }

    private void loadData() {
        Thread.ofVirtual().start(() -> {
            try {
                var list = dao.findAll();
                Platform.runLater(() -> { data.setAll(list); lblFormStatus.setText("Loaded " + list.size() + " locations"); });
            } catch (Exception e) {
                log.error("Load error: {}", e.getMessage(), e);
                Platform.runLater(() -> lblFormStatus.setText("Error: " + e.getMessage()));
            }
        });
    }

    @FXML public void saveLocation() {
        try {
            Location loc = selectedLocation != null ? selectedLocation : new Location();
            loc.setCode(txfCode.getText().trim());
            loc.setName(txfName.getText().trim());
            loc.setType(cmbType.getValue());
            loc.setDistrict(txfDistrict.getText().trim());
            loc.setState(txfState.getText().trim());
            loc.setLatitude(parseDouble(txfLat.getText(), 0));
            loc.setLongitude(parseDouble(txfLon.getText(), 0));
            loc.setCanvasX(parseDouble(txfCx.getText(), 0.5));
            loc.setCanvasY(parseDouble(txfCy.getText(), 0.5));
            loc.setActive(chkActive.isSelected());

            if (loc.getId() == 0) dao.insert(loc);
            else                  dao.update(loc);

            lblFormStatus.setText("✅ Saved: " + loc.getName());
            clearForm();
            loadData();
        } catch (Exception e) {
            lblFormStatus.setText("❌ Error: " + e.getMessage());
        }
    }

    @FXML public void deleteLocation() {
        if (selectedLocation == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete location: " + selectedLocation.getName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    dao.delete(selectedLocation.getId());
                    clearForm();
                    loadData();
                } catch (Exception e) { lblFormStatus.setText("❌ " + e.getMessage()); }
            }
        });
    }

    @FXML public void clearForm() {
        selectedLocation = null;
        txfCode.clear(); txfName.clear(); txfDistrict.clear(); txfState.clear();
        txfLat.clear(); txfLon.clear(); txfCx.clear(); txfCy.clear();
        cmbType.getSelectionModel().selectFirst();
        chkActive.setSelected(true);
        tblLocations.getSelectionModel().clearSelection();
        lblFormStatus.setText("Form cleared");
    }

    private void populateForm(Location loc) {
        selectedLocation = loc;
        txfCode.setText(loc.getCode());
        txfName.setText(loc.getName());
        cmbType.setValue(loc.getType());
        txfDistrict.setText(loc.getDistrict());
        txfState.setText(loc.getState());
        txfLat.setText(String.valueOf(loc.getLatitude()));
        txfLon.setText(String.valueOf(loc.getLongitude()));
        txfCx.setText(String.valueOf(loc.getCanvasX()));
        txfCy.setText(String.valueOf(loc.getCanvasY()));
        chkActive.setSelected(loc.isActive());
        lblFormStatus.setText("Editing: " + loc.getName());
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
