package com.pds.logistics.ui.controllers;

import com.pds.logistics.dao.InventoryDAO;
import com.pds.logistics.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {
    @FXML private TableView<InventoryRecord>            tblInventory;
    @FXML private TableColumn<InventoryRecord, String>  colId, colLocation, colGrain,
                                                         colQuantity, colAllocated, colAvailable, colDate;
    private final ObservableList<InventoryRecord> data = FXCollections.observableArrayList();
    private final InventoryDAO dao = new InventoryDAO();

    @Override public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c        -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colLocation.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getLocation().getName()));
        colGrain.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getGrainType().getDisplayName()));
        colQuantity.setCellValueFactory(c  -> new SimpleStringProperty("%.1f MT".formatted(c.getValue().getQuantityMT())));
        colAllocated.setCellValueFactory(c -> new SimpleStringProperty("%.1f MT".formatted(c.getValue().getAllocatedMT())));
        colAvailable.setCellValueFactory(c -> new SimpleStringProperty("%.1f MT".formatted(c.getValue().getEffectiveSupplyMT())));
        colDate.setCellValueFactory(c      -> new SimpleStringProperty(
                c.getValue().getReportDate() != null ? c.getValue().getReportDate().toString() : "–"));
        tblInventory.setItems(data);
        loadData();
    }

    @FXML public void loadData() {
        Thread.ofVirtual().start(() -> {
            try {
                var list = dao.findAllInventory();
                Platform.runLater(() -> data.setAll(list));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
