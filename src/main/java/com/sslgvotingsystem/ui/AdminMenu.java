package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(Scanner scanner) {
        this.userDao = new UserDao();
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ADMIN DASHBOARD (COMELEC ADVISER) ===");
            System.out.println("[1] View All Users");
            System.out.println("[2] Approve User Accounts");
            System.out.println("[3] Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> approveUserAccount();
                case "3" -> {
                    System.out.println("Logging out from Admin Dashboard...");
                    running = false;
                }
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n--- ALL REGISTERED USERS ---");
        printTableBox(users, false);
    }

    private void approveUserAccount() {
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("\nNo pending user accounts for approval.");
            return;
        }

        System.out.println("\n--- PENDING USER ACCOUNTS ---");
        printTableBox(pendingUsers, true);

        System.out.print("\nEnter User ID to approve (or type '0' to cancel): ");
        String input = scanner.nextLine().trim();

        try {
            int userId = Integer.parseInt(input);
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
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid input! Please enter a valid numeric ID.");
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