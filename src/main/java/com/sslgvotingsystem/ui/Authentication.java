package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.ActivityLogDao;
import com.sslgvotingsystem.dao.DatabaseConnection;
import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.logDao = new ActivityLogDao();
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n========== ELIGIO T. MONTE DE RAMOS HIGH SCHOOL ==========");
            System.out.println("=== SUPREME SECONDARY LEARNER'S GOVERNMENT VOTING ELECTION ===");
            System.out.println("\n[1] Register Account");
            System.out.println("[2] Login");
            System.out.println("[3] Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> registerAccount();
                case "2" -> loginAccount();
                case "3" -> {
                    System.out.println("Exiting system. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option! Please choose between 1, 2, or 3.");
            }
        }
    }

    private void registerAccount() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter School ID: ");
        String schoolId = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        if (fullName.isEmpty() || username.isEmpty() || schoolId.isEmpty() || password.isEmpty()) {
            System.out.println("\nAll fields (Full Name, Username, School ID, Password) are required.");
            return;
        }

        String role = readValidRoleRegistration();
        if (role == null) return;

        String gradeSection = null;
        if (role.equals("STUDENT")) {
            System.out.print("Enter Grade & Section (e.g., Grade 10 - Rizal): ");
            gradeSection = scanner.nextLine().trim();
        }

        String status = role.equals("ADMIN") ? "APPROVED" : "PENDING";

        boolean success = registerWithSchoolId(schoolId, fullName, username, password, role, status);
        if (success) {
            if (role.equals("STUDENT")) {
                saveVoterGradeSection(schoolId, gradeSection);
            }
            System.out.println("\nRegistration successful! School ID: " + schoolId + " | Account status: " + status);
        } else {
            System.out.println("\nRegistration failed. Username or School ID might already exist.");
        }
    }

    private boolean registerWithSchoolId(String schoolId, String fullName, String username, String password, String role, String status) {
        String sql = "INSERT INTO users (school_id, full_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schoolId);
            pstmt.setString(2, fullName);
            pstmt.setString(3, username);
            pstmt.setString(4, password);
            pstmt.setString(5, role);
            pstmt.setString(6, status);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error registering account: " + e.getMessage());
            return false;
        }
    }

    private void saveVoterGradeSection(String schoolId, String gradeSection) {
        String findUserSql = "SELECT id FROM users WHERE school_id = ?";
        String upsertVoterSql = """
            INSERT INTO voter (user_id, school_id, grade_section, has_voted)
            VALUES (?, ?, ?, 0)
            ON CONFLICT(user_id) DO UPDATE SET
                school_id = excluded.school_id,
                grade_section = excluded.grade_section
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findUserSql)) {

            findStmt.setString(1, schoolId);
            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    try (PreparedStatement upsertStmt = conn.prepareStatement(upsertVoterSql)) {
                        upsertStmt.setInt(1, userId);
                        upsertStmt.setString(2, schoolId);
                        upsertStmt.setString(3, gradeSection);
                        upsertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Warning: Could not save voter grade & section: " + e.getMessage());
        }
    }

    private String readValidRoleRegistration() {
        while (true) {
            System.out.println("Enter Role: ");
            System.out.println("[1] ADMIN: ");
            System.out.println("[2] STAFF: ");
            System.out.print("[3] STUDENT: ");
            String input = scanner.nextLine().trim().toUpperCase();

            switch (input) {
                case "1", "ADMIN" -> { return "ADMIN"; }
                case "2", "STAFF" -> { return "STAFF"; }
                case "3", "STUDENT" -> { return "STUDENT"; }
                default -> System.out.println("Invalid role! Please enter 1, 2, 3 OR type ADMIN, STAFF, STUDENT.");
            }
        }
     }

    private void loginAccount() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Enter School ID: ");
        String schoolId = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        User user = loginBySchoolId(schoolId, password);
        if (user == null) {
            System.out.println("\nLogin Failed! Invalid School ID or password.");
            return;
        }

        if (!user.status().equalsIgnoreCase("APPROVED")) {
            System.out.println("\nLogin Failed! Your account is still PENDING approval by an Admin.");
            return;
        }

        logDao.logAction(user.id(), "LOGIN", "School ID " + schoolId + " (" + user.fullName() + ") logged in.");

        System.out.println("\nLogin Successful! Welcome, " + user.fullName() + " [" + schoolId + "] (" + user.role() + ")");

        switch (user.role().toUpperCase()) {
            case "ADMIN" -> new AdminMenu(scanner, user).start();
            case "STAFF" -> new StaffMenu(scanner, user).start();
            case "STUDENT" -> new VoterMenu(scanner, user).start();
            default -> System.out.println("\nNo dashboard available for this role.");
        }
    }

    private User loginBySchoolId(String schoolId, String password) {
        String sql = "SELECT id, full_name, username, password, role, status FROM users WHERE (school_id = ? OR username = ?) AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schoolId);
            pstmt.setString(2, schoolId);
            pstmt.setString(3, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
        }
        return null;
    }
}