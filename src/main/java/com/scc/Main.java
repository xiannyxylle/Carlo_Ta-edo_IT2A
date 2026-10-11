package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.ui.ConsoleMenu;

public class Main {
    public static void main(String[] args) {
        // Initialize database and tables on startup
        DatabaseConnection.initializeDatabase();

        // Start the UI menu flow
        ConsoleMenu menu = new ConsoleMenu();
        menu.start();
    }
}