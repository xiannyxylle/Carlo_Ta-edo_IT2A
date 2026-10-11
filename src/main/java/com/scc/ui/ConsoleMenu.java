package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.Scanner;

public class ConsoleMenu {
    private final UserDao userDao = new UserDao();
    private final Scanner scanner = new Scanner(System.in);

    public void start() {
        while (true) {
            System.out.println("\n=== SSLG VOTING & ELECTION SYSTEM ===");
            System.out.println("[1] Register Account");
            System.out.println("[2] Login");
            System.out.println("[3] Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> handleRegister();
                case "2" -> handleLogin();
                case "3" -> {
                    System.out.println("Exiting application. Thank you!");
                    return; // Exit app
                }
                default -> System.out.println("Invalid choice! Palihug pagpili og tarong.");
            }
        }
    }

    private void handleRegister() {
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        System.out.println("Select Role:");
        System.out.println("[1] ADMIN (COMELEC Adviser)");
        System.out.println("[2] STAFF (Poll Clerk / Outgoing SSLG President)");
        System.out.println("[3] STUDENT (Voter)");
        System.out.print("Choose role option: ");
        String roleChoice = scanner.nextLine();

        String role = "STUDENT";
        if ("1".equals(roleChoice)) {
            role = "ADMIN";
        } else if ("2".equals(roleChoice)) {
            role = "STAFF";
        } else if ("3".equals(roleChoice)) {
            role = "STUDENT";
        }

        boolean success = userDao.registerUser(username, password, role);
        if (success) {
            System.out.println("Registration successful! Status is set to PENDING.");
        } else {
            System.out.println("Registration failed. Username might already exist.");
        }
    }

    private void handleLogin() {
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        User user = userDao.loginUser(username, password);
        if (user != null) {
            System.out.println("Login successful! Welcome, " + user.username() + " (" + user.role() + ")");
            System.out.println("Account Status: " + user.status());

            // 3. Logout / Exit session safely
            while (true) {
                System.out.print("Type 'logout' to end session and return to main menu: ");
                String logout = scanner.nextLine();
                if ("logout".equalsIgnoreCase(logout)) {
                    System.out.println("Successfully logged out.");
                    break;
                } else {
                    System.out.println("Invalid command.");
                }
            }
        } else {
            System.out.println("Invalid username or password.");
        }
    }
}