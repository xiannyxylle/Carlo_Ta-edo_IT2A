package com.sslgvotingsystem.dao;

import com.sslgvotingsystem.model.Position;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PositionDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean addPosition(Position position, int adminId) {
        String sql = "INSERT INTO positions (position_name, max_winners, sort_order) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, position.positionName());
            pstmt.setInt(2, position.maxWinners());
            pstmt.setInt(3, position.sortOrder());

            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "POSITION_ADDED",
                        "Added position: " + position.positionName() + " (Max winners: " + position.maxWinners() + ")"
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error adding position: " + e.getMessage());
        }
        return false;
    }

    public List<Position> getAllPositions() {
        List<Position> positions = new ArrayList<>();
        String sql = "SELECT id, position_name, max_winners, sort_order FROM positions ORDER BY sort_order ASC, id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                positions.add(new Position(
                        rs.getInt("id"),
                        rs.getString("position_name"),
                        rs.getInt("max_winners"),
                        rs.getInt("sort_order")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching positions: " + e.getMessage());
        }
        return positions;
    }

    public Position getPositionById(int id) {
        String sql = "SELECT id, position_name, max_winners, sort_order FROM positions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Position(
                            rs.getInt("id"),
                            rs.getString("position_name"),
                            rs.getInt("max_winners"),
                            rs.getInt("sort_order")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error finding position: " + e.getMessage());
        }
        return null;
    }

    public boolean updatePosition(Position position, int adminId) {
        String sql = "UPDATE positions SET position_name = ?, max_winners = ?, sort_order = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, position.positionName());
            pstmt.setInt(2, position.maxWinners());
            pstmt.setInt(3, position.sortOrder());
            pstmt.setInt(4, position.id());

            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "UPDATE_POSITION",
                        "Updated position ID " + position.id() + " to '" + position.positionName() + "'"
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error updating position: " + e.getMessage());
        }
        return false;
    }

    public boolean deletePosition(int id, int adminId) {
        String sql = "DELETE FROM positions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "POSITION_DELETED",
                        "Deleted position ID " + id
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error deleting position: " + e.getMessage());
        }
        return false;
    }
}