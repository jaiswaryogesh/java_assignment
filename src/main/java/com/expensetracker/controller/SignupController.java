package com.expensetracker.controller;

import com.expensetracker.repository.DatabaseHelper;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller for the User Registration view.
 */
public class SignupController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private void handleSignup(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showStatus("Please fill in all fields.", true);
            return;
        }

        if (username.length() < 3) {
            showStatus("Username must be at least 3 characters long.", true);
            return;
        }

        if (password.length() < 6) {
            showStatus("Password must be at least 6 characters long.", true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showStatus("Passwords do not match.", true);
            return;
        }

        boolean success = DatabaseHelper.registerUser(username, password);
        if (success) {
            showStatus("Account created successfully! Redirecting to login...", false);
            
            // Disable button to prevent double submits
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            confirmPasswordField.setDisable(true);

            // Pause for 1.5 seconds and transition back to login
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> switchScene("/fxml/login.fxml", "Expense Tracker - Login", event));
            pause.play();
        } else {
            showStatus("Username already exists. Please choose another.", true);
        }
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        switchScene("/fxml/login.fxml", "Expense Tracker - Login", event);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        if (isError) {
            statusLabel.getStyleClass().removeAll("label-success");
            if (!statusLabel.getStyleClass().contains("label-error")) {
                statusLabel.getStyleClass().add("label-error");
            }
        } else {
            statusLabel.getStyleClass().removeAll("label-error");
            if (!statusLabel.getStyleClass().contains("label-success")) {
                statusLabel.getStyleClass().add("label-success");
            }
        }
    }

    private void switchScene(String fxmlPath, String title, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Failed to load view: " + fxmlPath, true);
        }
    }
}
