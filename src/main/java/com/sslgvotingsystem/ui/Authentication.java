package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.model.User;

import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SSLG VOTING & ELECTION SYSTEM ===");
            System.out.println("[1] Register Account");
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

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        String role = readValidRoleRegistration();
        if (role == null) return;

        String status = "PENDING";
        if (role.equals("ADMIN")) {
            status = "APPROVED";
        }

        boolean success = userDao.registerUser(fullName, username, password, role, status);
        if (success) {
            System.out.println("\nRegistration successful! Account status: " + status);
        } else {
            System.out.println("\nRegistration failed. Username might already exist.");
        }
    }

    private String readValidRoleRegistration() {
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
    private void loginAccount() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        User user = userDao.loginUser(username, password);
        if (user == null) {
            System.out.println("\nLogin Failed! Invalid username or password.");
            return;
        }

        if (!user.status().equalsIgnoreCase("APPROVED")) {
            System.out.println("\nLogin Failed! Your account is still PENDING approval by an Admin.");
            return;
        }

        System.out.println("\nLogin Successful! Welcome, " + user.username() + " (" + user.role() + ")");

        switch (user.role().toUpperCase()) {
            case "ADMIN" -> {
                AdminMenu adminMenu = new AdminMenu(scanner, user);
                adminMenu.start();
            }
            case "STAFF" -> {
                System.out.println("\n>>> STAFF DASHBOARD (" + user.username() + ") <<< [Placeholder]");
            }
            case "STUDENT" -> {
                System.out.println("\n>>> STUDENT DASHBOARD (" + user.username() + ") <<< [Placeholder]");
            }
            default -> System.out.println("\nNo dashboard available for this role.");
        }
    }
}