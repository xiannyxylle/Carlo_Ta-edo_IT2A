package com.sslgvotingsystem.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ElectionSettingsDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public ElectionSettingsDao() {
        ensureTableExists();
    }

    private void ensureTableExists() {
        String createSql = """
            CREATE TABLE IF NOT EXISTS election_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                status TEXT NOT NULL DEFAULT 'OPEN'
            );
        """;
        String seedSql = "INSERT OR IGNORE INTO election_settings (id, status) VALUES (1, 'OPEN');";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            stmt.execute(seedSql);
        } catch (SQLException e) {
            System.out.println("Error initializing election settings: " + e.getMessage());
        }
    }

    public String getElectionStatus() {
        String sql = "SELECT status FROM election_settings WHERE id = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            System.out.println("Error reading election status: " + e.getMessage());
        }
        return "OPEN";
    }

    public boolean isElectionOpen() {
        return "OPEN".equalsIgnoreCase(getElectionStatus());
    }

    public boolean setElectionStatus(String newStatus, int adminId) {
        String normalized = newStatus.trim().toUpperCase();
        if (!normalized.equals("OPEN") && !normalized.equals("CLOSED")) {
            return false;
        }

        String sql = "UPDATE election_settings SET status = ? WHERE id = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalized);
            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "ELECTION_STATUS",
                        "Admin changed election period status to " + normalized
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error updating election status: " + e.getMessage());
        }
        return false;
    }
}