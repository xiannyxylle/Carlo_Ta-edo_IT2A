package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.ActivityLogDao;
import com.sslgvotingsystem.dao.CandidateDao;
import com.sslgvotingsystem.dao.ElectionSettingsDao;
import com.sslgvotingsystem.dao.PositionDao;
import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.dao.VoteDao;
import com.sslgvotingsystem.model.ActivityLog;
import com.sslgvotingsystem.model.Candidate;
import com.sslgvotingsystem.model.Position;
import com.sslgvotingsystem.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final PositionDao positionDao;
    private final CandidateDao candidateDao;
    private final VoteDao voteDao;
    private final ElectionSettingsDao settingsDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(Scanner scanner, User currentAdmin) {
        this.userDao = new UserDao();
        this.logDao = new ActivityLogDao();
        this.positionDao = new PositionDao();
        this.candidateDao = new CandidateDao();
        this.voteDao = new VoteDao();
        this.settingsDao = new ElectionSettingsDao();
        this.scanner = scanner;
        this.currentAdmin = currentAdmin;
    }

    public void start() {
        boolean running = true;

        while (running) {
            String electionStatus = settingsDao.getElectionStatus();

            System.out.println("\n==========================================");
            System.out.println("              ADMIN DASHBOARD             ");
            System.out.println("==========================================");
            System.out.println("Election Period Status: [" + electionStatus + "]");
            System.out.println("------------------------------------------");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. View & Export Election Results Summary");
            System.out.println("9. Manage Positions");
            System.out.println("10. Manage Candidates");
            System.out.println("11. Open / Close Election Period");
            System.out.println("12. Logout");
            System.out.print("Choose an option (1-12): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUserApprovals();
                case "3" -> approveUserAccount();
                case "4" -> addNewUser();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> deleteUserAccount();
                case "7" -> viewSystemAuditLogs();
                case "8" -> viewElectionResultsSummary();
                case "9" -> managePositions();
                case "10" -> manageCandidates();
                case "11" -> toggleElectionPeriod();
                case "12" -> {
                    System.out.println("Logging out from Admin Dashboard...");
                    running = false;
                }
                default -> System.out.println("Invalid option! Please choose between 1-12.");
            }
        }
    }

    private void toggleElectionPeriod() {
        String current = settingsDao.getElectionStatus();
        System.out.println("\n==========================================");
        System.out.println("        ELECTION PERIOD CONTROL");
        System.out.println("==========================================");
        System.out.println("Current Status: " + current);
        System.out.println("[1] OPEN Election (Allow student voting)");
        System.out.println("[2] CLOSE Election (Stop voting & publish results)");
        System.out.println("[0] Cancel");
        System.out.print("Choose an option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> {
                if (settingsDao.setElectionStatus("OPEN", currentAdmin.id())) {
                    System.out.println("\n[SUCCESS] Election period is now OPEN. Students can cast votes.");
                }
            }
            case "2" -> {
                if (settingsDao.setElectionStatus("CLOSED", currentAdmin.id())) {
                    System.out.println("\n[SUCCESS] Election period is now CLOSED. Voting is locked and official results are published to students.");
                }
            }
            case "0" -> System.out.println("Action cancelled.");
            default -> System.out.println("Invalid option.");
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n--- ALL REGISTERED USERS ---");
        printTableBox(users, false);
    }

    private void viewPendingUserApprovals() {
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("\nNo pending user accounts found.");
            return;
        }
        System.out.println("\n--- PENDING USER APPROVALS ---");
        printTableBox(pendingUsers, true);
    }

    private void approveUserAccount() {
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("\nNo pending user accounts for approval.");
            return;
        }

        System.out.println("\n--- APPROVE USER REGISTRATION ---");
        printTableBox(pendingUsers, true);

        int userId = readIntInput("Enter User ID to approve (or type '0' to cancel): ");
        if (userId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userId);
        if (exists) {
            if (userDao.approveUser(userId)) {
                System.out.println("\nSUCCESS: User ID " + userId + " has been successfully approved!");
            } else {
                System.out.println("\nFailed to approve user in the database.");
            }
        } else {
            System.out.println("\nInvalid User ID or user is not in the pending list.");
        }
    }

    private void addNewUser() {
        System.out.println("\n--- ADD NEW USER ---");
        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        String role = readValidRole();
        String status = readValidStatus();

        if (userDao.registerUser(fullName, username, password, role, status)) {
            System.out.println("\nSUCCESS: New user added successfully!");
        } else {
            System.out.println("\nFailed to add user. Username might already exist.");
        }
    }

    private void updateUserRoleAndStatus() {
        List<User> users = userDao.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("\nNo users found in the system.");
            return;
        }

        System.out.println("\n--- UPDATE USER ROLE / STATUS ---");
        printTableBox(users, false);

        int userId = readIntInput("Enter User ID to update (or type '0' to cancel): ");
        if (userId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (currentAdmin != null && userId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == userId);
        if (!exists) {
            System.out.println("Invalid User ID! User does not exist.");
            return;
        }

        String newRole = readValidRole();
        String newStatus = readValidStatus();

        if (userDao.updateUserRoleAndStatus(userId, newRole, newStatus)) {
            System.out.println("\nSUCCESS: User ID " + userId + " has been successfully updated!");
        } else {
            System.out.println("\nFailed to update user in the database.");
        }
    }

    private void deleteUserAccount() {
        System.out.println("\n--- DELETE USER ACCOUNT ---");
        List<User> users = userDao.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printTableBox(users, false);

        int userId = readIntInput("Enter User ID to delete (or type '0' to cancel): ");
        if (userId == 0) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (currentAdmin != null && userId == currentAdmin.id()) {
            System.out.println("WARNING: Deleting the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == userId);
        if (!exists) {
            System.out.println("Invalid User ID! User does not exist.");
            return;
        }

        System.out.print("Are you sure you want to delete User ID " + userId + "? (Y/N): ");
        String confirmation = scanner.nextLine().trim();
        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (userDao.deleteUser(userId, currentAdmin.id())) {
            System.out.println("\nSUCCESS: User ID " + userId + " has been deleted successfully!");
        } else {
            System.out.println("\nFailed to delete user.");
        }
    }

    private void viewSystemAuditLogs() {
        boolean viewing = true;
        while (viewing) {
            System.out.println("\n=== SYSTEM AUDIT LOGS ===");
            System.out.println("[1] View All System Logs");
            System.out.println("[2] Filter Logs by User ID");
            System.out.println("[3] Back to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> printLogsTable(logDao.getAllLogs());
                case "2" -> {
                    int userId = readIntInput("Enter User ID to filter: ");
                    printLogsTable(logDao.getLogsByUserId(userId));
                }
                case "3" -> viewing = false;
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }

    // ================= MANAGE POSITIONS =================
    private void managePositions() {
        boolean managing = true;
        while (managing) {
            System.out.println("\n=== MANAGE ELECTION POSITIONS ===");
            System.out.println("[1] View All Positions");
            System.out.println("[2] Add Position");
            System.out.println("[3] Update Position");
            System.out.println("[4] Delete Position");
            System.out.println("[5] Back to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> printPositionsTable(positionDao.getAllPositions());
                case "2" -> {
                    System.out.print("Enter Position Name: ");
                    String name = scanner.nextLine().trim();
                    int maxWinners = readIntInput("Enter Max Winners (e.g., 1): ");
                    int sortOrder = readIntInput("Enter Display Order (e.g., 1): ");
                    if (positionDao.addPosition(new Position(name, maxWinners, sortOrder), currentAdmin.id())) {
                        System.out.println("\nSUCCESS: Position added!");
                    } else {
                        System.out.println("\nFailed to add position.");
                    }
                }
                case "3" -> {
                    List<Position> positions = positionDao.getAllPositions();
                    if (positions.isEmpty()) {
                        System.out.println("\nNo positions available to update.");
                        break;
                    }
                    printPositionsTable(positions);
                    int posId = readIntInput("Enter Position ID to update (or 0 to cancel): ");
                    if (posId == 0) {
                        System.out.println("Update cancelled.");
                        break;
                    }

                    Position existing = positions.stream()
                            .filter(p -> p.id() == posId)
                            .findFirst()
                            .orElse(null);

                    if (existing == null) {
                        System.out.println("\nInvalid Position ID!");
                        break;
                    }

                    System.out.print("Enter New Position Name (leave blank to keep '" + existing.positionName() + "'): ");
                    String newName = scanner.nextLine().trim();
                    if (newName.isEmpty()) newName = existing.positionName();

                    System.out.print("Enter New Max Winners (leave blank to keep '" + existing.maxWinners() + "'): ");
                    String maxInput = scanner.nextLine().trim();
                    int newMaxWinners = existing.maxWinners();
                    if (!maxInput.isEmpty()) {
                        try {
                            newMaxWinners = Integer.parseInt(maxInput);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number! Keeping previous Max Winners.");
                        }
                    }

                    System.out.print("Enter New Display Order (leave blank to keep '" + existing.sortOrder() + "'): ");
                    String orderInput = scanner.nextLine().trim();
                    int newSortOrder = existing.sortOrder();
                    if (!orderInput.isEmpty()) {
                        try {
                            newSortOrder = Integer.parseInt(orderInput);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number! Keeping previous Display Order.");
                        }
                    }

                    Position updated = new Position(existing.id(), newName, newMaxWinners, newSortOrder);
                    if (positionDao.updatePosition(updated, currentAdmin.id())) {
                        System.out.println("\nSUCCESS: Position updated!");
                    } else {
                        System.out.println("\nFailed to update position.");
                    }
                }
                case "4" -> {
                    printPositionsTable(positionDao.getAllPositions());
                    int posId = readIntInput("Enter Position ID to delete (or 0 to cancel): ");
                    if (posId != 0) {
                        if (positionDao.deletePosition(posId, currentAdmin.id())) {
                            System.out.println("\nSUCCESS: Position deleted!");
                        } else {
                            System.out.println("\nFailed to delete position.");
                        }
                    }
                }
                case "5" -> managing = false;
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private void printPositionsTable(List<Position> positions) {
        if (positions.isEmpty()) {
            System.out.println("\nNo positions found.");
            return;
        }
        String line = "+-------+---------------------------+-------------+-------+";
        System.out.println(line);
        System.out.printf("| %-5s | %-25s | %-11s | %-5s |%n", "ID", "Position Name", "Max Winners", "Order");
        System.out.println(line);
        for (Position p : positions) {
            System.out.printf("| %-5d | %-25s | %-11d | %-5d |%n",
                    p.id(), p.positionName(), p.maxWinners(), p.sortOrder());
        }
        System.out.println(line);
    }

    // ================= MANAGE CANDIDATES =================
    private void manageCandidates() {
        boolean managing = true;
        while (managing) {
            System.out.println("\n=== MANAGE ELECTION CANDIDATES ===");
            System.out.println("[1] View All Candidates");
            System.out.println("[2] Register Candidate");
            System.out.println("[3] Edit Candidate Platform / Party List");
            System.out.println("[4] Remove Disqualified Candidate");
            System.out.println("[5] Back to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> printCandidatesTable(candidateDao.getAllCandidates());
                case "2" -> {
                    List<User> eligibleStudents = userDao.getAllUsers().stream()
                            .filter(u -> "STUDENT".equalsIgnoreCase(u.role())
                                    && "APPROVED".equalsIgnoreCase(u.status()))
                            .toList();

                    if (eligibleStudents.isEmpty()) {
                        System.out.println("\nNo approved STUDENT accounts available. Have the student register and get approved first.");
                        break;
                    }

                    System.out.println("\n--- ELIGIBLE APPROVED STUDENTS ---");
                    printTableBox(eligibleStudents, false);

                    int targetUserId = readIntInput("Enter Student User ID to register as Candidate (or 0 to cancel): ");
                    if (targetUserId == 0) {
                        System.out.println("Candidate registration cancelled.");
                        break;
                    }

                    boolean validStudent = eligibleStudents.stream().anyMatch(u -> u.id() == targetUserId);
                    if (!validStudent) {
                        System.out.println("\nInvalid User ID! Select an APPROVED STUDENT from the list above.");
                        break;
                    }

                    printPositionsTable(positionDao.getAllPositions());
                    int positionId = readIntInput("Enter Position ID: ");
                    System.out.print("Enter Party List: ");
                    String partyList = scanner.nextLine().trim();
                    System.out.print("Enter Platform: ");
                    String platform = scanner.nextLine().trim();

                    if (candidateDao.addCandidate(new Candidate(targetUserId, positionId, partyList, platform), currentAdmin.id())) {
                        System.out.println("\nSUCCESS: Candidate registered!");
                    } else {
                        System.out.println("\nFailed to register candidate.");
                    }
                }
                case "3" -> {
                    List<Candidate> candidates = candidateDao.getAllCandidates();
                    if (candidates.isEmpty()) {
                        System.out.println("\nNo candidates available to edit.");
                        break;
                    }
                    printCandidatesTable(candidates);
                    int candId = readIntInput("Enter Candidate ID to edit (or 0 to cancel): ");
                    if (candId == 0) break;

                    Candidate existing = candidates.stream()
                            .filter(c -> c.id() == candId)
                            .findFirst()
                            .orElse(null);

                    if (existing == null) {
                        System.out.println("\nInvalid Candidate ID!");
                        break;
                    }

                    System.out.print("Enter New Party List (leave blank to keep '" + existing.partyList() + "'): ");
                    String newParty = scanner.nextLine().trim();
                    if (newParty.isEmpty()) newParty = existing.partyList();

                    System.out.print("Enter New Platform (leave blank to keep current): ");
                    String newPlatform = scanner.nextLine().trim();
                    if (newPlatform.isEmpty()) newPlatform = existing.platform();

                    Candidate updated = new Candidate(
                            existing.id(),
                            existing.userId(),
                            existing.positionId(),
                            newParty,
                            newPlatform,
                            existing.candidateName(),
                            existing.positionName()
                    );

                    if (candidateDao.updateCandidate(updated, currentAdmin.id())) {
                        System.out.println("\nSUCCESS: Candidate details updated!");
                    } else {
                        System.out.println("\nFailed to update candidate.");
                    }
                }
                case "4" -> {
                    printCandidatesTable(candidateDao.getAllCandidates());
                    int candId = readIntInput("Enter Candidate ID to remove (or 0 to cancel): ");
                    if (candId != 0) {
                        if (candidateDao.deleteCandidate(candId, currentAdmin.id())) {
                            System.out.println("\nSUCCESS: Candidate removed!");
                        } else {
                            System.out.println("\nFailed to remove candidate.");
                        }
                    }
                }
                case "5" -> managing = false;
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private void printCandidatesTable(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            System.out.println("\nNo candidates found.");
            return;
        }
        String line = "+-------+----------------------+------------------------+-----------------+--------------------------+";
        System.out.println(line);
        System.out.printf("| %-5s | %-20s | %-22s | %-15s | %-24s |%n",
                "ID", "Position", "Candidate Name", "Party List", "Platform");
        System.out.println(line);
        for (Candidate c : candidates) {
            System.out.printf("| %-5d | %-20s | %-22s | %-15s | %-24s |%n",
                    c.id(), c.positionName(), c.candidateName(), c.partyList(), c.platform());
        }
        System.out.println(line);
    }

    // ================= VIEW & EXPORT ELECTION RESULTS SUMMARY =================
    private void viewElectionResultsSummary() {
        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        System.out.println("\n==================================================================================================");
        System.out.println("                           ELECTION RESULTS SUMMARY & WINNERS");
        System.out.println("==================================================================================================");
        System.out.printf("Election Status: [%s] | Voter Turnout: %d / %d Approved Students (%.1f%%)%n",
                settingsDao.getElectionStatus(), totalVoted, totalStudents, percentage);
        System.out.println("--------------------------------------------------------------------------------------------------");

        List<ReportExporter.ResultRow> summary = ReportExporter.buildOfficialSummary(positionDao, candidateDao, voteDao);
        if (summary.isEmpty()) {
            System.out.println("No candidates or votes recorded yet.");
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

        System.out.println("Select Export / Download Format:");
        System.out.println("  1. PDF");
        System.out.println("  2. CSV");
        System.out.println("  3. Excel formats");
        System.out.println("  0. Do not export");
        System.out.print("Enter choice (1-3, or 0): ");
        String exportChoice = scanner.nextLine().trim();

        String savedPath = null;
        String formatLabel = null;

        switch (exportChoice) {
            case "1" -> {
                formatLabel = "PDF";
                savedPath = ReportExporter.exportSummaryToPdf(positionDao, candidateDao, voteDao);
            }
            case "2" -> {
                formatLabel = "CSV";
                savedPath = ReportExporter.exportSummaryToCsv(positionDao, candidateDao, voteDao);
            }
            case "3" -> {
                formatLabel = "EXCEL";
                savedPath = ReportExporter.exportSummaryToExcel(positionDao, candidateDao, voteDao);
            }
            case "0" -> {
                System.out.println("Export skipped.");
                return;
            }
            default -> {
                System.out.println("Invalid format choice. Export skipped.");
                return;
            }
        }

        if (savedPath != null) {
            logDao.logAction(currentAdmin.id(), "REPORT_EXPORTED", "Exported Election Results Summary (" + formatLabel + ")");
            System.out.println("\n[SUCCESS] " + formatLabel + " report exported successfully!");
            System.out.println("File saved at: " + savedPath);
        } else {
            System.out.println("\n[ERROR] Failed to export " + formatLabel + " report.");
        }
    }

    private void printLogsTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("\nNo audit logs found.");
            return;
        }
        String line = "+-------+----------+--------------------+-------------------------------------+----------------------+";
        System.out.println(line);
        System.out.printf("| %-5s | %-8s | %-18s | %-35s | %-20s |%n",
                "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(line);
        for (ActivityLog log : logs) {
            System.out.printf("| %-5d | %-8d | %-18s | %-35s | %-20s |%n",
                    log.id(), log.userId(), log.actionType(), log.description(), log.timestamp());
        }
        System.out.println(line);
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid numeric ID.");
            }
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role ([1] ADMIN, [2] STAFF, [3] STUDENT): ");
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "1", "ADMIN" -> { return "ADMIN"; }
                case "2", "STAFF" -> { return "STAFF"; }
                case "3", "STUDENT" -> { return "STUDENT"; }
                default -> System.out.println("Invalid role! Please enter 1, 2, 3 OR type ADMIN, STAFF, STUDENT.");
            }
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter status (APPROVED, PENDING): ");
            String status = scanner.nextLine().trim().toUpperCase();
            if (status.equals("APPROVED") || status.equals("PENDING")) {
                return status;
            }
            System.out.println("Invalid status! Please enter APPROVED or PENDING.");
        }
    }

    private void printTableBox(List<User> users, boolean isPendingOnly) {
        String line = "+-----+----------------------+-----------------+------------+------------+";
        if (isPendingOnly) {
            line = "+-----+----------------------+-----------------+------------+";
            System.out.println(line);
            System.out.printf("| %-3s | %-20s | %-15s | %-10s |%n", "ID", "Full Name", "Username", "Role");
            System.out.println(line);
            for (User u : users) {
                System.out.printf("| %-3d | %-20s | %-15s | %-10s |%n",
                        u.id(), u.fullName(), u.username(), u.role());
            }
        } else {
            System.out.println(line);
            System.out.printf("| %-3s | %-20s | %-15s | %-10s | %-10s |%n",
                    "ID", "Full Name", "Username", "Role", "Status");
            System.out.println(line);
            for (User u : users) {
                System.out.printf("| %-3d | %-20s | %-15s | %-10s | %-10s |%n",
                        u.id(), u.fullName(), u.username(), u.role(), u.status());
            }
        }
        System.out.println(line);
    }
}