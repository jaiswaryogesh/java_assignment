package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import com.expensetracker.model.User;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles SQLite database connection, table creation, and CRUD operations.
 */
public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:expense_tracker.db";

    static {
        // Initialize database and tables
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL" +
                    ");");

            // Create Expenses table
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "description TEXT, " +
                    "date TEXT NOT NULL, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ");");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // --- User Authentication Methods ---

    /**
     * Hashes a password using SHA-256.
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error hashing password", ex);
        }
    }

    /**
     * Registers a new user. Returns true if successful, false if username already exists.
     */
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // SQLite unique constraint error code is usually 19 (or contains UNIQUE)
            return false;
        }
    }

    /**
     * Authenticates a user. Returns the User object if successful, null otherwise.
     */
    public static User loginUser(String username, String password) {
        String sql = "SELECT id, username, password FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Expense Management CRUD Methods ---

    /**
     * Adds an expense to the database.
     */
    public static boolean addExpense(Expense expense) {
        String sql = "INSERT INTO expenses (user_id, amount, category, description, date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, expense.getUserId());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getCategory());
            pstmt.setString(4, expense.getDescription());
            pstmt.setString(5, expense.getDate().toString());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing expense in the database.
     */
    public static boolean updateExpense(Expense expense) {
        String sql = "UPDATE expenses SET amount = ?, category = ?, description = ?, date = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, expense.getAmount());
            pstmt.setString(2, expense.getCategory());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4, expense.getDate().toString());
            pstmt.setInt(5, expense.getId());
            pstmt.setInt(6, expense.getUserId());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes an expense.
     */
    public static boolean deleteExpense(int expenseId, int userId) {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves filtered and searched expenses.
     */
    public static List<Expense> getFilteredExpenses(int userId, String search, String category, LocalDate startDate, LocalDate endDate) {
        List<Expense> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, user_id, amount, category, description, date FROM expenses WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (description LIKE ? OR category LIKE ?)");
            params.add("%" + search.trim() + "%");
            params.add("%" + search.trim() + "%");
        }
        if (category != null && !category.equals("All")) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (startDate != null) {
            sql.append(" AND date >= ?");
            params.add(startDate.toString());
        }
        if (endDate != null) {
            sql.append(" AND date <= ?");
            params.add(endDate.toString());
        }
        
        sql.append(" ORDER BY date DESC, id DESC");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Expense exp = new Expense();
                    exp.setId(rs.getInt("id"));
                    exp.setUserId(rs.getInt("user_id"));
                    exp.setAmount(rs.getDouble("amount"));
                    exp.setCategory(rs.getString("category"));
                    exp.setDescription(rs.getString("description"));
                    exp.setDate(LocalDate.parse(rs.getString("date")));
                    list.add(exp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Calculates the total expenses of a user.
     */
    public static double getTotalExpenses(int userId) {
        String sql = "SELECT SUM(amount) AS total FROM expenses WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Calculates total expenses of a user for the current month.
     */
    public static double getCurrentMonthExpenses(int userId) {
        String startOfMonth = LocalDate.now().withDayOfMonth(1).toString();
        String endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).toString();
        String sql = "SELECT SUM(amount) AS total FROM expenses WHERE user_id = ? AND date >= ? AND date <= ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, startOfMonth);
            pstmt.setString(3, endOfMonth);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Returns a breakdown of expenses by category (for PieChart).
     */
    public static Map<String, Double> getCategoryBreakdown(int userId) {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT category, SUM(amount) AS total FROM expenses WHERE user_id = ? GROUP BY category";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Returns monthly spending summary of the last 6 months (for BarChart/Visuals).
     */
    public static Map<String, Double> getMonthlySummary(int userId) {
        Map<String, Double> map = new HashMap<>();
        // Group by year-month
        String sql = "SELECT strftime('%Y-%m', date) AS month_str, SUM(amount) AS total " +
                     "FROM expenses WHERE user_id = ? " +
                     "GROUP BY month_str " +
                     "ORDER BY month_str DESC LIMIT 6";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("month_str"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}
