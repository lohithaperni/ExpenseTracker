package com.expensetracker.ui.support;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbSeeder {

    private final String jdbcUrl;

    public DbSeeder(Path dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public void reset() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            conn.createStatement().execute("DELETE FROM expenses");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset expenses table", e);
        }
    }

    public void seedBaseline() {
        insert(10.00,  "Food",      "2026-01-01", "coffee");
        insert(25.00,  "Transport", "2026-01-15", "uber ride");
        insert(40.00,  "Food",      "2026-02-01", "groceries");
        insert(100.00, "Bills",     "2026-02-10", "electric");
        insert(5.00,   "Food",      "2026-02-10", "");
    }

    public void insert(double amount, String category, String date, String note) {
        String sql = "INSERT INTO expenses (amount, category, expense_date, note) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            var stmt = conn.prepareStatement(sql);
            stmt.setDouble(1, amount);
            stmt.setString(2, category);
            stmt.setString(3, date);
            stmt.setString(4, note);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert expense", e);
        }
    }
}
