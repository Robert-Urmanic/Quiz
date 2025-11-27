package org.example.Application;

import org.example.database.DatabaseConnection;

import java.sql.*;
import java.util.Scanner;

public class Application {
    private static Connection connection;
    private static int iRows = 0;
    private static int subchapterRow = 0;

    public static void run(){
        connection = DatabaseConnection.connect("jdbc:sqlserver://URMANICR-DH4241:1433;databaseName=Quiz;encrypt=false", "testDB", "testDB");

        System.out.println("What do you want to do?");
        System.out.println("----------------------------------");
        System.out.println("1. Add a chapter -----------------");
        System.out.println("2. Add a subchapter --------------");
        System.out.println("3. Add a question ----------------");

        Scanner input = new Scanner(System.in);
    // žádná kontrola na input, budu to používat jen já...
        switch(Integer.parseInt(input.nextLine())){
            case 1:
                System.out.println("Enter the chapter name:");
                insert(input, "INSERT INTO chapter(name) VALUES(?)", 0);
                break;
            case 2:
                System.out.println("Select a chapter to link:");
                try {
                    Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    ResultSet rs = stmt.executeQuery("SELECT Name, Id FROM Chapter");
                    System.out.println("0. - None");
                    while (rs.next()) {
                        iRows++;
                        System.out.println(iRows + ". - " + rs.getString(1));
                    }

                    subchapterRow = Integer.parseInt(input.nextLine());
                    rs.absolute(subchapterRow);
                    System.out.println("Type subchapter name:");
                    if (subchapterRow == 0)
                        insert(input, "INSERT INTO subchapter(name) VALUES(?)", 0); else {
                            insert(input, "INSERT INTO subchapter(name, chapterId) VALUES(?, ?)", rs.getInt(2));
                    }

                    iRows = 0;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 3:
//                insertQuestion(input);
                break;
            default:
                System.out.println("Nothing found");
                break;
        }
    }

    private static void insert(Scanner input, String query, int chapterId) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, input.nextLine());
            if (chapterId != 0)
                pstmt.setInt(2, chapterId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
