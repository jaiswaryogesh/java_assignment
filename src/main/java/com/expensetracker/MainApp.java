package com.expensetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load Login View
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            stage.setTitle("Expense Tracker - Login");
            stage.setScene(scene);
            stage.setMinWidth(760);
            stage.setMinHeight(580);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fatal error starting application: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
