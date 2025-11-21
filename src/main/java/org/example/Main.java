package org.example;

import org.example.database.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.connect("jdbc:sqlserver://URMANICR-DH4241:1433;databaseName=Quiz;encrypt=false", "testDB", "testDB");

        System.out.println("What do you want to do?");
        System.out.println("------------------------------");
        System.out.println("1. Add a chapter -------------");
        System.out.println("2. Add a subchapter ----------");
        System.out.println("3. Add a quiz ----------------");
    }
}