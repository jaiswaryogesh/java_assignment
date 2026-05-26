package com.expensetracker.model;

import java.time.LocalDate;

/**
 * Model class representing an Expense.
 */
public class Expense {
    private int id;
    private int userId;
    private double amount;
    private String category;
    private String description;
    private LocalDate date;

    public Expense() {
    }

    public Expense(int id, int userId, double amount, String category, String description, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // Helper for table display format
    public String getFormattedAmount() {
        return String.format("$%.2f", amount);
    }
}
