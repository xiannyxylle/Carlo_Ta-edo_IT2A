package com.sslgvotingsystem.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {

        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                school_id TEXT UNIQUE,
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
                position_name TEXT UNIQUE NOT NULL,
                max_winners INTEGER NOT NULL DEFAULT 1,
                sort_order INTEGER NOT NULL DEFAULT 0
            );
        """;

        String createCandidateTable = """
            CREATE TABLE IF NOT EXISTS candidate (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL UNIQUE,
                position_id INTEGER NOT NULL,
                party_list TEXT NOT NULL,
                platform TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE
            );
        """;

        String createVoterTable = """
            CREATE TABLE IF NOT EXISTS voter (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL UNIQUE,
                grade_section TEXT,
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
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (voter_id) REFERENCES voter(id) ON DELETE CASCADE,
                FOREIGN KEY (candidate_id) REFERENCES candidate(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE CASCADE,
                UNIQUE(voter_id, position_id)
            );
        """;

        String createActivityLogsTable = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;

        String createElectionSettingsTable = """
            CREATE TABLE IF NOT EXISTS election_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                status TEXT NOT NULL DEFAULT 'OPEN'
            );
        """;

        String seedDefaultElectionStatus = """
            INSERT OR IGNORE INTO election_settings (id, status)
            VALUES (1, 'OPEN');
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable SQLite foreign key enforcement
            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute(createUsersTable);
            stmt.execute(createPositionsTable);
            stmt.execute(createCandidateTable);
            stmt.execute(createVoterTable);
            stmt.execute(createVotesTable);
            stmt.execute(createActivityLogsTable);
            stmt.execute(createElectionSettingsTable);

            // Safe migrations for existing databases
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN school_id TEXT;");
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_school_id ON users(school_id);");
            } catch (SQLException ignored) {
                // school_id column already exists
            }

            try {
                stmt.execute("ALTER TABLE voter ADD COLUMN grade_section TEXT;");
            } catch (SQLException ignored) {
                // grade_section column already exists
            }

            try {
                stmt.execute("ALTER TABLE voter ADD COLUMN school_id TEXT;");
            } catch (SQLException ignored) {
                // school_id column already exists in voter
            }

            stmt.execute(seedDefaultElectionStatus);

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }
}