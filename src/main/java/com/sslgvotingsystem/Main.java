package com.sslgvotingsystem;

import com.sslgvotingsystem.dao.DatabaseInitializer;
import com.sslgvotingsystem.dao.UserDao;
import com.sslgvotingsystem.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        DatabaseInitializer.initializeDatabase();


        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);


        Authentication auth = new Authentication(userDao, scanner);
        auth.start();

        scanner.close();
    }
}