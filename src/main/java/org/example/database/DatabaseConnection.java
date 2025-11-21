package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;

public class DatabaseConnection {
    private String url; // = "jdbc:sqlserver://URMANICR-DH4241:1433;databaseName=Quiz;encrypt=false";
    private String user; // = "testDB";
    private String password; // = "testDB";

    DatabaseConnection(String url, String user, String password) {
        this.url        = url;
        this.user       = user;
        this.password   = password;

        connect();
    }

    public void connect() {
        connect(this.url, this.user, this.password);
    }

    public static void connect(String url, String user, String password) {
        try (
            Connection conn = DriverManager.getConnection(url, user, password)) {
            // System.out.println("Connected to SQL Server successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
