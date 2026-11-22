package com.sslgvotingsystem.dao;

import com.sslgvotingsystem.model.Candidate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public boolean addCandidate(Candidate candidate, int adminId) {
        String sql = "INSERT INTO candidate (user_id, position_id, party_list, platform) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, candidate.userId());
            pstmt.setInt(2, candidate.positionId());
            pstmt.setString(3, candidate.partyList());
            pstmt.setString(4, candidate.platform());

            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "CANDIDATE_ADDED",
                        "Registered User ID " + candidate.userId() + " as candidate for Position ID " + candidate.positionId()
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error adding candidate (check if user is already registered as a candidate): " + e.getMessage());
        }
        return false;
    }

    public List<Candidate> getAllCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        String sql = """
            SELECT c.id, c.user_id, c.position_id, c.party_list, c.platform,
                   u.full_name AS candidate_name, p.position_name
            FROM candidate c
            JOIN users u ON c.user_id = u.id
            JOIN positions p ON c.position_id = p.id
            ORDER BY p.sort_order ASC, p.id ASC, u.full_name ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                candidates.add(new Candidate(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("position_id"),
                        rs.getString("party_list"),
                        rs.getString("platform"),
                        rs.getString("candidate_name"),
                        rs.getString("position_name")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching candidates: " + e.getMessage());
        }
        return candidates;
    }

    public List<Candidate> getCandidatesByPosition(int positionId) {
        List<Candidate> candidates = new ArrayList<>();
        String sql = """
            SELECT c.id, c.user_id, c.position_id, c.party_list, c.platform,
                   u.full_name AS candidate_name, p.position_name
            FROM candidate c
            JOIN users u ON c.user_id = u.id
            JOIN positions p ON c.position_id = p.id
            WHERE c.position_id = ?
            ORDER BY u.full_name ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, positionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    candidates.add(new Candidate(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("position_id"),
                            rs.getString("party_list"),
                            rs.getString("platform"),
                            rs.getString("candidate_name"),
                            rs.getString("position_name")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching candidates for position: " + e.getMessage());
        }
        return candidates;
    }

    public boolean updateCandidate(Candidate candidate, int adminId) {
        String sql = "UPDATE candidate SET position_id = ?, party_list = ?, platform = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, candidate.positionId());
            pstmt.setString(2, candidate.partyList());
            pstmt.setString(3, candidate.platform());
            pstmt.setInt(4, candidate.id());

            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "CANDIDATE_UPDATED",
                        "Updated candidate ID " + candidate.id()
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error updating candidate: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteCandidate(int candidateId, int adminId) {
        String sql = "DELETE FROM candidate WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, candidateId);
            if (pstmt.executeUpdate() > 0) {
                activityLogDao.logAction(
                        adminId,
                        "CANDIDATE_DELETED",
                        "Removed candidate ID " + candidateId
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error deleting candidate: " + e.getMessage());
        }
        return false;
    }
}