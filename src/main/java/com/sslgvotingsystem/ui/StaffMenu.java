package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.ActivityLogDao;
import com.sslgvotingsystem.dao.CandidateDao;
import com.sslgvotingsystem.dao.PositionDao;
import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.dao.VoteDao;
import com.sslgvotingsystem.dao.DatabaseConnection;
import com.sslgvotingsystem.model.Candidate;
import com.sslgvotingsystem.model.Position;
import com.sslgvotingsystem.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class StaffMenu {

    private final Scanner scanner;
    private final User currentStaff;
    private final UserDao userDao = new UserDao();
    private final PositionDao positionDao = new PositionDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final VoteDao voteDao = new VoteDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public StaffMenu(Scanner scanner, User currentStaff) {
        this.scanner = scanner;
        this.currentStaff = currentStaff;
    }

    public void start() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("        SSLG ELECTION STAFF PORTAL");
            System.out.println("==========================================");
            System.out.println("Staff  : " + currentStaff.fullName() + " (@" + currentStaff.username() + ")");
            System.out.println("Role   : " + currentStaff.role());
            System.out.println("------------------------------------------");
            System.out.println("1. View Pending Student Approvals");
            System.out.println("2. View Voter Turnout List");
            System.out.println("3. View Official Positions");
            System.out.println("4. View Official Candidates & Platforms");
            System.out.println("5. View Live Election Tally & Turnout");
            System.out.println("0. Logout");
            System.out.println("==========================================");

            int choice = readIntInput("Enter your choice: ");

            switch (choice) {
                case 1 -> viewPendingStudents();
                case 2 -> viewVoterTurnoutList();
                case 3 -> viewPositions();
                case 4 -> viewCandidates();
                case 5 -> viewLiveTally();
                case 0 -> {
                    activityLogDao.logAction(currentStaff.id(), "LOGOUT", "Staff logged out: " + currentStaff.username());
                    System.out.println("Logged out from Staff Portal.");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void viewPendingStudents() {
        List<User> pendingStudents = userDao.getPendingUsers().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.role()))
                .toList();

        System.out.println("\n==============================================================");
        System.out.println("                 PENDING STUDENT APPROVALS");
        System.out.println("==============================================================");

        if (pendingStudents.isEmpty()) {
            System.out.println("No pending student registrations found.");
            System.out.println("==============================================================");
            return;
        }

        System.out.printf("%-5s | %-22s | %-15s | %-10s%n",
                "ID", "FULL NAME", "USERNAME", "ROLE");
        System.out.println("--------------------------------------------------------------");
        for (User u : pendingStudents) {
            System.out.printf("%-5d | %-22s | %-15s | %-10s%n",
                    u.id(), u.fullName(), u.username(), u.role());
        }
        System.out.println("==============================================================");
    }

    private void viewVoterTurnoutList() {
        String sql = """
                SELECT COALESCE(v.school_id, u.school_id, 'N/A') AS school_id,
                       u.full_name,
                       COALESCE(v.grade_section, 'N/A') AS grade_section,
                       COALESCE(v.has_voted, 0) AS has_voted
                FROM users u
                LEFT JOIN voter v ON u.id = v.user_id
                WHERE UPPER(u.role) = 'STUDENT'
                ORDER BY grade_section ASC, u.full_name ASC
                """;

        int totalStudents = 0;
        int votedCount = 0;

        System.out.println("\n===============================================================================");
        System.out.println("                              VOTER TURNOUT LIST");
        System.out.println("===============================================================================");
        System.out.printf("%-18s | %-24s | %-18s | %-13s%n",
                "SCHOOL ID", "FULL NAME", "GRADE & SECTION", "STATUS");
        System.out.println("-------------------------------------------------------------------------------");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                totalStudents++;
                String schoolId = rs.getString("school_id");
                String fullName = rs.getString("full_name");
                String gradeSection = rs.getString("grade_section");
                boolean hasVoted = rs.getInt("has_voted") == 1;

                if (hasVoted) {
                    votedCount++;
                }

                String statusLabel = hasVoted ? "VOTED" : "NOT YET VOTED";
                System.out.printf("%-18s | %-24s | %-18s | %-13s%n",
                        schoolId, fullName, gradeSection, statusLabel);
            }

            if (totalStudents == 0) {
                System.out.println("No student voter records found.");
            }

            double rate = totalStudents > 0 ? (votedCount * 100.0) / totalStudents : 0.0;
            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf("Turnout Summary: %d / %d voted (%.1f%%)%n",
                    votedCount, totalStudents, rate);
            System.out.println("===============================================================================");

        } catch (SQLException e) {
            System.out.println("Error loading voter turnout list: " + e.getMessage());
        }
    }

    private void viewPositions() {
        List<Position> positions = positionDao.getAllPositions();

        System.out.println("\n==============================================================");
        System.out.println("                  OFFICIAL SSLG POSITIONS");
        System.out.println("==============================================================");

        if (positions.isEmpty()) {
            System.out.println("No election positions configured yet.");
            System.out.println("==============================================================");
            return;
        }

        System.out.printf("%-5s | %-28s | %-12s | %-8s%n",
                "ID", "POSITION NAME", "MAX WINNERS", "ORDER");
        System.out.println("--------------------------------------------------------------");
        for (Position p : positions) {
            System.out.printf("%-5d | %-28s | %-12d | %-8d%n",
                    p.id(), p.positionName(), p.maxWinners(), p.sortOrder());
        }
        System.out.println("==============================================================");
    }

    private void viewCandidates() {
        List<Candidate> candidates = candidateDao.getAllCandidates();

        System.out.println("\n=======================================================================================");
        System.out.println("                       OFFICIAL CANDIDATES & PLATFORMS");
        System.out.println("=======================================================================================");

        if (candidates.isEmpty()) {
            System.out.println("No candidates have been registered yet.");
            System.out.println("=======================================================================================");
            return;
        }

        System.out.printf("%-5s | %-20s | %-22s | %-15s | %-25s%n",
                "ID", "POSITION", "CANDIDATE NAME", "PARTY LIST", "PLATFORM");
        System.out.println("---------------------------------------------------------------------------------------");
        for (Candidate c : candidates) {
            System.out.printf("%-5d | %-20s | %-22s | %-15s | %-25s%n",
                    c.id(),
                    c.positionName(),
                    c.candidateName(),
                    c.partyList(),
                    c.platform() != null ? c.platform() : "N/A");
        }
        System.out.println("=======================================================================================");
    }

    private void viewLiveTally() {
        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        System.out.println("\n===============================================================================");
        System.out.println("                    LIVE ELECTION TALLY & VOTER TURNOUT");
        System.out.println("===============================================================================");
        System.out.printf("Voter Turnout: %d / %d Approved Students (%.1f%%)%n", totalVoted, totalStudents, percentage);
        System.out.println("-------------------------------------------------------------------------------");

        List<String[]> tally = voteDao.getElectionTally();
        if (tally.isEmpty()) {
            System.out.println("No candidates or votes recorded yet.");
            System.out.println("===============================================================================");
            return;
        }

        System.out.printf("%-22s | %-24s | %-16s | %-6s%n",
                "POSITION", "CANDIDATE NAME", "PARTY LIST", "VOTES");
        System.out.println("-------------------------------------------------------------------------------");
        for (String[] row : tally) {
            System.out.printf("%-22s | %-24s | %-16s | %-6s%n",
                    row[0], row[1], row[2], row[3]);
        }
        System.out.println("===============================================================================");
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }
}