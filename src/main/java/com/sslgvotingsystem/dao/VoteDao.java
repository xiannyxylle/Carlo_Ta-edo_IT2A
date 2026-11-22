package com.sslgvotingsystem.dao;

import com.sslgvotingsystem.model.Vote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoteDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    // Ensures the user has a corresponding row in the voter table and returns voter.id
    public int getOrCreateVoterId(int userId) {
        String selectSql = "SELECT id FROM voter WHERE user_id = ?";
        String insertSql = "INSERT INTO voter (user_id, has_voted) VALUES (?, 0)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, userId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, userId);
                insertStmt.executeUpdate();
                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving/creating voter profile: " + e.getMessage());
        }
        return -1;
    }

    // Checks if a student (by users.id) has already cast their ballot
    public boolean hasUserVoted(int userId) {
        String sql = "SELECT has_voted FROM voter WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("has_voted") == 1;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking voter status: " + e.getMessage());
        }
        return false;
    }

    // Atomically submits all votes on a student's ballot and marks has_voted = 1
    public boolean submitBallot(int userId, List<Vote> ballotChoices) {
        if (ballotChoices == null || ballotChoices.isEmpty()) {
            return false;
        }

        int voterId = getOrCreateVoterId(userId);
        if (voterId == -1 || hasUserVoted(userId)) {
            return false;
        }

        String insertVoteSql = "INSERT INTO votes (voter_id, candidate_id, position_id) VALUES (?, ?, ?)";
        String markVotedSql = "UPDATE voter SET has_voted = 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement voteStmt = conn.prepareStatement(insertVoteSql)) {
                for (Vote vote : ballotChoices) {
                    voteStmt.setInt(1, voterId);
                    voteStmt.setInt(2, vote.candidateId());
                    voteStmt.setInt(3, vote.positionId());
                    voteStmt.addBatch();
                }
                voteStmt.executeBatch();
            }

            try (PreparedStatement updateVoterStmt = conn.prepareStatement(markVotedSql)) {
                updateVoterStmt.setInt(1, voterId);
                updateVoterStmt.executeUpdate();
            }

            conn.commit();

            // Audit log records participation ONLY—preserving strict ballot secrecy
            activityLogDao.logAction(
                    userId,
                    "BALLOT_CAST",
                    "Student User ID " + userId + " successfully submitted their official SSLG ballot."
            );
            return true;

        } catch (SQLException e) {
            System.out.println("Error submitting ballot: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("Error closing connection: " + closeEx.getMessage());
                }
            }
        }
        return false;
    }

    // Returns formatted tally rows: [Position Name, Candidate Name, Party List, Total Votes]
    public List<String[]> getElectionTally() {
        List<String[]> results = new ArrayList<>();
        String sql = """
            SELECT p.position_name, u.full_name AS candidate_name, c.party_list, COUNT(v.id) AS total_votes
            FROM candidate c
            JOIN users u ON c.user_id = u.id
            JOIN positions p ON c.position_id = p.id
            LEFT JOIN votes v ON c.id = v.candidate_id
            GROUP BY c.id, p.position_name, u.full_name, c.party_list
            ORDER BY p.sort_order ASC, p.id ASC, total_votes DESC, u.full_name ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                results.add(new String[]{
                        rs.getString("position_name"),
                        rs.getString("candidate_name"),
                        rs.getString("party_list"),
                        String.valueOf(rs.getInt("total_votes"))
                });
            }
        } catch (SQLException e) {
            System.out.println("Error generating election tally: " + e.getMessage());
        }
        return results;
    }

    // Returns [Total Approved Students, Total Voted] for turnout tracking
    public int[] getVoterTurnoutStats() {
        String totalStudentsSql = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND status = 'APPROVED'";
        String totalVotedSql = "SELECT COUNT(*) FROM voter WHERE has_voted = 1";
        int totalStudents = 0;
        int totalVoted = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs1 = stmt.executeQuery(totalStudentsSql)) {
                if (rs1.next()) totalStudents = rs1.getInt(1);
            }
            try (ResultSet rs2 = stmt.executeQuery(totalVotedSql)) {
                if (rs2.next()) totalVoted = rs2.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error calculating turnout stats: " + e.getMessage());
        }
        return new int[]{totalStudents, totalVoted};
    }
}