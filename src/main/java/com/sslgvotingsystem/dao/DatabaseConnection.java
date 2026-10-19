package com.sslgvotingsystem.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    // Siguraduha nga ang ngalan sa database nimo kay angay sa imong proyekto
    private static final String URL = "jdbc:sqlite:sslgvoting.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        // Enable SQLite Foreign Key constraints on every connection initialization[cite: 10]
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        return conn;
    }
}