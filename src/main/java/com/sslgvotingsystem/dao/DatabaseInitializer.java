package com.sslgvotingsystem.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        // SQL queries para sa 6 ka tables sa atong sistema
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING'
            );
        """;

        String createPositionsTable = """
            CREATE TABLE IF NOT EXISTS positions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                position_name TEXT UNIQUE NOT NULL
            );
        """;

        String createCandidateTable = """
            CREATE TABLE IF NOT EXISTS candidate (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                party_list TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE
            );
        """;

        String createVoterTable = """
            CREATE TABLE IF NOT EXISTS voter (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                has_voted INTEGER DEFAULT 0,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;

        String createVotesTable = """
            CREATE TABLE IF NOT EXISTS votes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                voter_id INTEGER NOT NULL,
                candidate_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                FOREIGN KEY (voter_id) REFERENCES voter(id) ON DELETE CASCADE,
                FOREIGN KEY (candidate_id) REFERENCES candidate(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE
            );
        """;

        String createActivityLogsTable = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // I-execute ang paghimo sa mga tables
            stmt.execute(createUsersTable);
            stmt.execute(createPositionsTable);
            stmt.execute(createCandidateTable);
            stmt.execute(createVoterTable);
            stmt.execute(createVotesTable);
            stmt.execute(createActivityLogsTable);

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }
}