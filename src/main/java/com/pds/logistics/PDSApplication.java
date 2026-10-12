package com.pds.logistics;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX Application entry point for the PDS Logistics Optimization System.
 */
public class PDSApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(PDSApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting PDS Logistics Optimization System");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainWindow.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("PDS Logistics Optimization System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        log.info("Application window displayed");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
