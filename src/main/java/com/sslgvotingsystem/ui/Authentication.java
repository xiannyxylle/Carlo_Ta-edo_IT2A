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
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Thank you for using SSLG Voting System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        System.out.println("Select Role:");
        System.out.println("[1] Admin (Comelec Adviser)");
        System.out.println("[2] Staff (Poll Clerk / Outgoing SSLG President)");
        System.out.println("[3] Student (Voter)");
        System.out.print("Choose role: ");

        String roleChoice = scanner.nextLine().trim();
        String role = switch (roleChoice) {
            case "1" -> "ADMIN";
            case "2" -> "STAFF";
            case "3" -> "STUDENT";
            default -> null;
        };

        if (role == null) {
            System.out.println("Invalid role selected! Registration cancelled.");
            return;
        }

        boolean success = userDao.registerUser(fullName, username, password, role);
        if (success) {
            if (role.equals("ADMIN")) {
                System.out.println("Registration successful! Account is AUTOMATICALLY APPROVED.");
            } else {
                System.out.println("Registration successful! Your account status is PENDING approval.");
            }
        } else {
            System.out.println("Registration failed! Username might already exist.");
        }
    }

    private void login() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        User user = userDao.loginUser(username, password);

        if (user == null) {
            System.out.println("Invalid username or password!");
            return;
        }

        if ("PENDING".equalsIgnoreCase(user.status())) {
            System.out.println("Your account is still PENDING for admin approval. Please wait for confirmation.");
            return;
        }

        System.out.println("\nLogin Successful! Welcome, " + user.fullName() + " (" + user.role() + ")");
        routeUserByRole(user);
    }

    private void routeUserByRole(User user) {
        switch (user.role().toUpperCase()) {
            case "ADMIN" -> {
                AdminMenu adminMenu = new AdminMenu(scanner);
                adminMenu.start();
            }
            case "STAFF" -> System.out.println("\n>>> STAFF DASHBOARD (" + user.fullName() + ") <<< [Placeholder]");
            case "STUDENT" -> System.out.println("\n>>> STUDENT VOTER DASHBOARD (" + user.fullName() + ") <<< [Placeholder]");
            default -> System.out.println("Unknown role! Access denied.");
        }
    }
}