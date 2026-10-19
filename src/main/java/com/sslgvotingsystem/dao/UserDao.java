package com.sslgvotingsystem.dao;

import com.sslgvotingsystem.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean registerUser(String fullName, String username, String password, String role) {
        String query = "INSERT INTO users (full_name, username, password, role, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role);

            // ADMIN is APPROVED automatically, others are PENDING
            String status = role.equals("ADMIN") ? "APPROVED" : "PENDING";
            pstmt.setString(5, status);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    public User loginUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

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
            System.err.println("Error during login: " + e.getMessage());
        }
        return null;
    }


    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                userList.add(new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return userList;
    }

    public List<User> getPendingUsers() {
        List<User> pendingList = new ArrayList<>();
        String query = "SELECT * FROM users WHERE status = 'PENDING' ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                pendingList.add(new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pending users: " + e.getMessage());
        }
        return pendingList;
    }


    public boolean approveUser(int userId) {
        String query = "UPDATE users SET status = 'APPROVED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error approving user: " + e.getMessage());
            return false;
        }
    }
}