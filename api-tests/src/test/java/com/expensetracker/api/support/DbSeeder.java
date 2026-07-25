package com.expensetracker.api.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Seeds and resets the SQLite test database via JDBC.
 *
 * Baseline dataset (from test plan section 5.3):
 *   2026-01-01  Food    10.00   "Coffee"
 *   2026-01-15  Travel 120.00  "Train ticket"
 *   2026-02-01  Food    25.50   "groceries"
 *   2026-02-10  Bills   60.00   "Internet"
 *   2026-02-10  Food    15.00   "Coffee beans"
 */
public class DbSeeder {

    private final String jdbcUrl;

    public DbSeeder(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    /** Creates the expenses table if it does not already exist. */
    public void createSchema() throws Exception {
        try (Connection conn = connect(); Statement s = conn.createStatement()) {
            s.execute(
                "CREATE TABLE IF NOT EXISTS expenses (" +
                "    id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    amount       REAL    NOT NULL," +
                "    category     TEXT    NOT NULL," +
                "    expense_date TEXT    NOT NULL," +
                "    note         TEXT" +
                ")"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Reset / Seed
    // -------------------------------------------------------------------------

    /** Deletes all rows from the expenses table. */
    public void reset() throws Exception {
        try (Connection conn = connect(); Statement s = conn.createStatement()) {
            s.execute("DELETE FROM expenses");
        }
    }

    /** Resets the table and inserts the canonical 5-row baseline dataset. */
    public void seedBaseline() throws Exception {
        reset();
        insert(10.00,  "Food",   "2026-01-01", "Coffee");
        insert(120.00, "Travel", "2026-01-15", "Train ticket");
        insert(25.50,  "Food",   "2026-02-01", "groceries");
        insert(60.00,  "Bills",  "2026-02-10", "Internet");
        insert(15.00,  "Food",   "2026-02-10", "Coffee beans");
    }

    /**
     * Inserts a single expense row.
     * Useful for test-specific data that goes beyond the baseline.
     */
    public void insert(double amount, String category, String date, String note) throws Exception {
        String sql = "INSERT INTO expenses (amount, category, expense_date, note) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, category);
            ps.setString(3, date);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private Connection connect() throws Exception {
        return DriverManager.getConnection(jdbcUrl);
    }
}
