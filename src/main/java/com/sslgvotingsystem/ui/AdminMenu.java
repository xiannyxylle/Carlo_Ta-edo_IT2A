package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(Scanner scanner, User currentAdmin) {
        this.userDao = new UserDao();
        this.scanner = scanner;
        this.currentAdmin = currentAdmin;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n==========================================");
            System.out.println("              ADMIN DASHBOARD             ");
            System.out.println("==========================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. Logout");
            System.out.print("Choose an option (1-7): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUserApprovals();
                case "3" -> approveUserAccount();
                case "4" -> addNewUser();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> deleteUserAccount();
                case "7" -> {
                    System.out.println("Logging out from Admin Dashboard...");
                    running = false;
                }
                default -> System.out.println("Invalid option! Please choose between 1-7.");
            }
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
            boolean success = userDao.approveUser(userId);
            if (success) {
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

        boolean success = userDao.registerUser(fullName, username, password, role, status);
        if (success) {
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

        boolean success = userDao.updateUserRoleAndStatus(userId, newRole, newStatus);
        if (success) {
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

        System.out.println("User deletion feature placeholder.");
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
            System.out.printf("| %-3s | %-20s | %-15s | %-10s | %-10s |%n", "ID", "Full Name", "Username", "Role", "Status");
            System.out.println(line);
            for (User u : users) {
                System.out.printf("| %-3d | %-20s | %-15s | %-10s | %-10s |%n",
                        u.id(), u.fullName(), u.username(), u.role(), u.status());
            }
        }
        System.out.println(line);
    }
}