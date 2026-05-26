package com.expensetracker.controller;

import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.DatabaseHelper;
import com.expensetracker.util.CsvExporter;
import com.expensetracker.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the Main Dashboard view.
 */
public class DashboardController {

    // --- FXML Bindings ---
    @FXML private Label welcomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label monthlyExpensesLabel;
    @FXML private Label topCategoryLabel;

    // Filters
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private DatePicker startDateFilter;
    @FXML private DatePicker endDateFilter;
    @FXML private Button clearFiltersButton;

    // TableView
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, LocalDate> dateColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TableColumn<Expense, Double> amountColumn;

    // Charts
    @FXML private PieChart categoryPieChart;

    // Form
    @FXML private Label formTitleLabel;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryField;
    @FXML private TextField descriptionField;
    @FXML private DatePicker dateField;
    @FXML private Label formStatusLabel;
    
    // Form buttons
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button cancelEditButton;
    @FXML private Button exportButton;
    @FXML private Button logoutButton;

    // Track editing state
    private Expense selectedExpense = null;
    private final String[] categories = {"Food", "Transport", "Utilities", "Entertainment", "Housing", "Health", "Education", "Others"};

    @FXML
    public void initialize() {
        // Set up filters
        categoryFilter.getItems().add("All");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("All");

        // Set up form combo box
        categoryField.getItems().addAll(categories);

        // Prepopulate form date with today
        dateField.setValue(LocalDate.now());

        // Set up TableView columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(tc -> new TableCell<Expense, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", value));
                }
            }
        });

        // Add selection listener to Table
        expenseTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectExpense(newValue);
            }
        });

        // Add listeners to filters to refresh table automatically
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTableAndStats());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTableAndStats());
        startDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTableAndStats());
        endDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTableAndStats());

        // Initial Load
        refreshDashboard();
    }

    /**
     * Refreshes everything on the dashboard.
     */
    private void refreshDashboard() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
        refreshTableAndStats();
    }

    /**
     * Queries database for filtered list and refreshes tables, cards, and charts.
     */
    private void refreshTableAndStats() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        int userId = currentUser.getId();
        String search = searchField.getText();
        String cat = categoryFilter.getValue();
        LocalDate start = startDateFilter.getValue();
        LocalDate end = endDateFilter.getValue();

        // 1. Fetch expenses & populate table
        List<Expense> expenses = DatabaseHelper.getFilteredExpenses(userId, search, cat, start, end);
        expenseTable.getItems().setAll(expenses);

        // 2. Fetch general stats for cards (overall, not filtered)
        double totalSum = DatabaseHelper.getTotalExpenses(userId);
        totalExpensesLabel.setText(String.format("$%.2f", totalSum));

        double monthlySum = DatabaseHelper.getCurrentMonthExpenses(userId);
        monthlyExpensesLabel.setText(String.format("$%.2f", monthlySum));

        Map<String, Double> breakdown = DatabaseHelper.getCategoryBreakdown(userId);
        String topCat = "N/A";
        double maxVal = -1.0;
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            if (entry.getValue() > maxVal) {
                maxVal = entry.getValue();
                topCat = entry.getKey();
            }
        }
        if (maxVal > 0) {
            topCategoryLabel.setText(String.format("%s ($%.0f)", topCat, maxVal));
        } else {
            topCategoryLabel.setText("N/A");
        }

        // 3. Update Pie Chart
        categoryPieChart.getData().clear();
        if (!breakdown.isEmpty()) {
            for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
                double pct = (entry.getValue() / (totalSum > 0 ? totalSum : 1)) * 100;
                PieChart.Data slice = new PieChart.Data(
                    String.format("%s ($%.2f, %.0f%%)", entry.getKey(), entry.getValue(), pct),
                    entry.getValue()
                );
                categoryPieChart.getData().add(slice);
            }
        }
    }

    /**
     * Fills the form fields with the selected expense for editing.
     */
    private void selectExpense(Expense expense) {
        selectedExpense = expense;
        formTitleLabel.setText("Edit Expense");
        amountField.setText(String.valueOf(expense.getAmount()));
        categoryField.setValue(expense.getCategory());
        descriptionField.setText(expense.getDescription());
        dateField.setValue(expense.getDate());

        // Toggle button visibilities
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        saveButton.setText("Update");
        hideFormError();
    }

    /**
     * Clears the selected item and resets the form to "Add Expense".
     */
    @FXML
    private void handleCancelEdit() {
        selectedExpense = null;
        expenseTable.getSelectionModel().clearSelection();
        
        formTitleLabel.setText("Add Expense");
        amountField.clear();
        categoryField.setValue(null);
        descriptionField.clear();
        dateField.setValue(LocalDate.now());

        // Toggle buttons back
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        saveButton.setText("Save");
        hideFormError();
    }

    /**
     * Triggers clearing all filter controls.
     */
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All");
        startDateFilter.setValue(null);
        endDateFilter.setValue(null);
        refreshTableAndStats();
    }

    /**
     * Logic for saving or updating an expense.
     */
    @FXML
    private void handleSaveExpense() {
        String amountStr = amountField.getText().trim();
        String category = categoryField.getValue();
        String description = descriptionField.getText().trim();
        LocalDate date = dateField.getValue();

        // Validations
        if (amountStr.isEmpty()) {
            showFormError("Amount is required.");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showFormError("Amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            showFormError("Invalid amount format. Use numbers (e.g. 12.50).");
            return;
        }

        if (category == null || category.isEmpty()) {
            showFormError("Please select a category.");
            return;
        }

        if (date == null) {
            showFormError("Please choose a date.");
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        if (selectedExpense == null) {
            // INSERT Mode
            Expense newExp = new Expense(0, currentUser.getId(), amount, category, description, date);
            if (DatabaseHelper.addExpense(newExp)) {
                handleCancelEdit();
                refreshTableAndStats();
            } else {
                showFormError("Database error. Failed to add expense.");
            }
        } else {
            // UPDATE Mode
            selectedExpense.setAmount(amount);
            selectedExpense.setCategory(category);
            selectedExpense.setDescription(description);
            selectedExpense.setDate(date);

            if (DatabaseHelper.updateExpense(selectedExpense)) {
                handleCancelEdit();
                refreshTableAndStats();
            } else {
                showFormError("Database error. Failed to update expense.");
            }
        }
    }

    /**
     * Logic for deleting a selected expense.
     */
    @FXML
    private void handleDeleteExpense() {
        if (selectedExpense == null) return;
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Expense");
        confirm.setHeaderText("Delete selected expense?");
        confirm.setContentText("Are you sure you want to permanently delete this expense: \"" 
                + selectedExpense.getDescription() + "\" ($" + selectedExpense.getAmount() + ")?");

        // Style the confirmation alert to follow dark theme somewhat
        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("main-bg");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (DatabaseHelper.deleteExpense(selectedExpense.getId(), currentUser.getId())) {
                handleCancelEdit();
                refreshTableAndStats();
            } else {
                showFormError("Failed to delete expense.");
            }
        }
    }

    /**
     * Exports current table listing to CSV.
     */
    @FXML
    private void handleExportCsv() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        ObservableList<Expense> currentItems = expenseTable.getItems();
        if (currentItems.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("Cannot export empty table");
            alert.setContentText("There are no expenses matching your current filters to export.");
            styleDialog(alert);
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Expenses to CSV");
        fileChooser.setInitialFileName("expenses_report.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Stage stage = (Stage) expenseTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            if (CsvExporter.exportToCsv(currentItems, file)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Complete");
                alert.setHeaderText("Export Successful");
                alert.setContentText("Your expenses have been exported to:\n" + file.getAbsolutePath());
                styleDialog(alert);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("An error occurred");
                alert.setContentText("Could not write CSV file. Please ensure you have write permissions.");
                styleDialog(alert);
                alert.showAndWait();
            }
        }
    }

    /**
     * Log out of current session.
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.cleanSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Expense Tracker - Login");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showFormError(String msg) {
        formStatusLabel.setText(msg);
        formStatusLabel.setVisible(true);
        formStatusLabel.setManaged(true);
    }

    private void hideFormError() {
        formStatusLabel.setVisible(false);
        formStatusLabel.setManaged(false);
    }

    private void styleDialog(Alert alert) {
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dp.getStyleClass().add("main-bg");
    }
}
