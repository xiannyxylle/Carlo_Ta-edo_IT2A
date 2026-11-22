package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.ActivityLogDao;
import com.sslgvotingsystem.dao.CandidateDao;
import com.sslgvotingsystem.dao.ElectionSettingsDao;
import com.sslgvotingsystem.dao.PositionDao;
import com.sslgvotingsystem.dao.VoteDao;
import com.sslgvotingsystem.model.Candidate;
import com.sslgvotingsystem.model.Position;
import com.sslgvotingsystem.model.User;
import com.sslgvotingsystem.model.Vote;
import com.sslgvotingsystem.ui.ReportExporter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class VoterMenu {

    private final Scanner scanner;
    private final User currentUser;
    private final PositionDao positionDao = new PositionDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final VoteDao voteDao = new VoteDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ElectionSettingsDao settingsDao = new ElectionSettingsDao();

    public VoterMenu(Scanner scanner, User currentUser) {
        this.scanner = scanner;
        this.currentUser = currentUser;
    }

    public void start() {
        while (true) {
            boolean hasVoted = voteDao.hasUserVoted(currentUser.id());
            String electionStatus = settingsDao.getElectionStatus();

            System.out.println("\n==========================================");
            System.out.println("        SSLG STUDENT VOTER PORTAL");
            System.out.println("==========================================");
            System.out.println("Voter    : " + currentUser.fullName() + " (@" + currentUser.username() + ")");
            System.out.println("Status   : " + (hasVoted ? "VOTED (Official Ballot Recorded)" : "NOT YET VOTED"));
            System.out.println("Election : [" + electionStatus + "]");
            System.out.println("------------------------------------------");
            System.out.println("1. View Official Candidates & Platforms");
            System.out.println("2. Cast Official SSLG Ballot");
            System.out.println("3. Check My Voting Status");
            System.out.println("4. View Published Election Results");
            System.out.println("0. Logout");
            System.out.println("==========================================");

            int choice = readIntInput("Enter your choice: ");

            switch (choice) {
                case 1 -> viewCandidates();
                case 2 -> castBallot();
                case 3 -> checkVotingStatus();
                case 4 -> viewPublishedResults();
                case 0 -> {
                    activityLogDao.logAction(currentUser.id(), "LOGOUT", "Student logged out: " + currentUser.username());
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void viewCandidates() {
        List<Candidate> candidates = candidateDao.getAllCandidates();
        if (candidates.isEmpty()) {
            System.out.println("\n==========================================");
            System.out.println("      OFFICIAL CANDIDATES & PLATFORMS");
            System.out.println("==========================================");
            System.out.println("No candidates have been registered yet.");
            System.out.println("==========================================");
            return;
        }

        System.out.println("\n=======================================================================================");
        System.out.println("                       OFFICIAL CANDIDATES & PLATFORMS");
        System.out.println("=======================================================================================");
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

    private void castBallot() {
        if (!settingsDao.isElectionOpen()) {
            System.out.println("\n==========================================");
            System.out.println("          OFFICIAL SSLG BALLOT");
            System.out.println("==========================================");
            System.out.println("[NOTICE] The election period is currently CLOSED.");
            System.out.println("Ballot casting is no longer available.");
            System.out.println("==========================================");
            return;
        }

        if (voteDao.hasUserVoted(currentUser.id())) {
            System.out.println("\n==========================================");
            System.out.println("          OFFICIAL SSLG BALLOT");
            System.out.println("==========================================");
            System.out.println("[NOTICE] You have already cast your official ballot.");
            System.out.println("Multiple voting is strictly prohibited.");
            System.out.println("==========================================");
            return;
        }

        List<Position> positions = positionDao.getAllPositions();
        if (positions.isEmpty()) {
            System.out.println("\n==========================================");
            System.out.println("          OFFICIAL SSLG BALLOT");
            System.out.println("==========================================");
            System.out.println("No election positions are configured yet.");
            System.out.println("Please contact the SSLG COMELEC.");
            System.out.println("==========================================");
            return;
        }

        List<Vote> selectedVotes = new ArrayList<>();
        List<String> summaryLines = new ArrayList<>();

        System.out.println("\n==========================================");
        System.out.println("          OFFICIAL SSLG BALLOT");
        System.out.println("==========================================");
        System.out.println("Instructions: Enter the Candidate ID for each position, or 0 to abstain.");
        System.out.println("==========================================");

        for (Position pos : positions) {
            List<Candidate> posCandidates = candidateDao.getCandidatesByPosition(pos.id());
            if (posCandidates.isEmpty()) {
                System.out.println("\n==========================================");
                System.out.println("  " + pos.positionName().toUpperCase());
                System.out.println("==========================================");
                System.out.println("  (No candidates running)");
                System.out.println("==========================================");
                continue;
            }

            System.out.println("\n==========================================");
            System.out.println("  " + pos.positionName().toUpperCase());
            System.out.println("==========================================");
            for (Candidate c : posCandidates) {
                System.out.printf("  [%d] %s (%s)%n", c.id(), c.candidateName(), c.partyList());
            }
            System.out.println("==========================================");

            while (true) {
                int chosenId = readIntInput("Select Candidate ID for " + pos.positionName() + " (0 to abstain): ");
                if (chosenId == 0) {
                    summaryLines.add(pos.positionName() + ": [ABSTAIN]");
                    break;
                }

                Candidate matched = posCandidates.stream()
                        .filter(c -> c.id() == chosenId)
                        .findFirst()
                        .orElse(null);

                if (matched != null) {
                    selectedVotes.add(new Vote(0, matched.id(), pos.id()));
                    summaryLines.add(pos.positionName() + ": " + matched.candidateName() + " (" + matched.partyList() + ")");
                    break;
                } else {
                    System.out.println("Invalid Candidate ID for " + pos.positionName() + ". Please select from the list above.");
                }
            }
        }

        if (selectedVotes.isEmpty()) {
            System.out.println("\n==========================================");
            System.out.println("          OFFICIAL SSLG BALLOT");
            System.out.println("==========================================");
            System.out.println("You abstained from all positions.");
            System.out.println("Ballot was not submitted.");
            System.out.println("==========================================");
            return;
        }

        System.out.println("\n==========================================");
        System.out.println("          REVIEW YOUR SELECTIONS");
        System.out.println("==========================================");
        for (String line : summaryLines) {
            System.out.println(" • " + line);
        }
        System.out.println("==========================================");
        System.out.print("Confirm and submit official ballot? (YES/NO): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("YES")) {
            boolean success = voteDao.submitBallot(currentUser.id(), selectedVotes);
            if (success) {
                System.out.println("\n[SUCCESS] Thank you, " + currentUser.fullName() + "! Your vote has been securely recorded.");
                printOfficialVotingReceipt(summaryLines);
            } else {
                System.out.println("\n[ERROR] Failed to submit your ballot. Please try again or contact an administrator.");
            }
        } else {
            System.out.println("\nBallot submission cancelled. No votes were recorded.");
        }
    }

    private void printOfficialVotingReceipt(List<String> summaryLines) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
        String receiptNo = String.format("sslg-%d-%04d", now.getYear(), currentUser.id());

        System.out.println("\n==================================================");
        System.out.println("          SSLG OFFICIAL VOTING RECEIPT");
        System.out.println("==================================================");
        System.out.println("Receipt No. : " + receiptNo);
        System.out.println("Date & Time : " + timestamp);
        System.out.println("Voter Name  : " + currentUser.fullName() + " (@" + currentUser.username() + ")");
        System.out.println("Status      : VERIFIED & RECORDED");
        System.out.println("--------------------------------------------------");
        System.out.println("OFFICIAL BALLOT SUMMARY:");
        for (String line : summaryLines) {
            System.out.println(" • " + line);
        }
        System.out.println("--------------------------------------------------");
        System.out.println("Keep this reference number as proof of voting.");
        System.out.println("==================================================");
    }

    private void checkVotingStatus() {
        boolean hasVoted = voteDao.hasUserVoted(currentUser.id());
        System.out.println("\n==========================================");
        System.out.println("              VOTING STATUS");
        System.out.println("==========================================");
        System.out.println("Student Name : " + currentUser.fullName());
        System.out.println("Ballot Status: " + (hasVoted ? "SUBMITTED (Verified)" : "PENDING (Not yet voted)"));
        System.out.println("==========================================");
    }

    private void viewPublishedResults() {
        if (settingsDao.isElectionOpen()) {
            System.out.println("\n==========================================");
            System.out.println("       PUBLISHED ELECTION RESULTS");
            System.out.println("==========================================");
            System.out.println("[NOTICE] Official results are not yet released.");
            System.out.println("Results will be published once the COMELEC Admin closes the election.");
            System.out.println("==========================================");
            return;
        }

        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        System.out.println("\n==================================================================================================");
        System.out.println("                        OFFICIAL PUBLISHED SSLG ELECTION RESULTS");
        System.out.println("==================================================================================================");
        System.out.printf("Overall Voter Turnout: %d / %d Approved Students (%.1f%%)%n", totalVoted, totalStudents, percentage);
        System.out.println("--------------------------------------------------------------------------------------------------");

        List<ReportExporter.ResultRow> summary = ReportExporter.buildOfficialSummary(positionDao, candidateDao, voteDao);
        if (summary.isEmpty()) {
            System.out.println("No election results available.");
            System.out.println("==================================================================================================");
            return;
        }

        System.out.printf("| %-20s | %-22s | %-15s | %-6s | %-17s |%n",
                "Position", "Candidate Name", "Party List", "Votes", "Remarks");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (ReportExporter.ResultRow row : summary) {
            System.out.printf("| %-20s | %-22s | %-15s | %-6d | %-17s |%n",
                    row.positionName, row.candidateName, row.partyList, row.votes, row.remarks);
        }
        System.out.println("==================================================================================================");
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