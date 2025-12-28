package org.example.Application;

import org.example.database.DatabaseConnection;

import java.sql.*;
import java.util.Scanner;

public class Application {
    private static Connection connection;
    private static int iRows = 0;
    private static int chapterId = 0;
    private static int subChapterId = 0;
    private static ResultSet rs;
    private static boolean run = true;

    public static void run() {
        connection = DatabaseConnection.connect("jdbc:sqlserver://URMANICR-DH4241:1433;databaseName=Quiz;encrypt=false", "testDB", "testDB");

        while (run) {

            System.out.println("What do you want to do?");
            System.out.println("----------------------------------");
            System.out.println("1. Add a chapter -----------------");
            System.out.println("2. Add a subchapter --------------");
            System.out.println("3. Add a question ----------------");
            System.out.println("4. See all chapter questions -----");
            System.out.println("9. EXIT");

            Scanner input = new Scanner(System.in);
            // žádná kontrola na input, budu to používat jen já...
            try {
                switch (Integer.parseInt(input.nextLine())) {
                    case 1:
                        System.out.println("Enter the chapter name:");
                        insert(input, "INSERT INTO chapter(name) VALUES(?)", 0, 0);
                        break;
                    case 2:
                        chapterId = listItemsToChoose("chapter", rs, input);
                        System.out.println("Type subchapter name:");
                        if (chapterId == 0)
                            insert(input, "INSERT INTO subchapter(name) VALUES(?)", 0, 0);
                        else {
                            insert(input, "INSERT INTO subchapter(name, chapterId) VALUES(?, ?)", chapterId, 0);
                        }
                        break;
                    case 3:
                        System.out.println("Enter the chapter name:");
                        chapterId = listItemsToChoose("chapter", rs, input);
                        System.out.println("Enter the subchapter name:");
                        subChapterId = listItemsToChoose("subchapter", rs, input);
                        System.out.println("Enter the question:");
                        insert(input, "INSERT INTO question(question, chapterId, subchapterId) VALUES(?,?,?)", chapterId, subChapterId);
                        break;
                    case 4:
                        rs = getAllQuestions();
                        while (rs.next()) {
                            System.out.println("Chapter: " + rs.getString("chname") + "\n\tSubchapter: " + rs.getString("suname") + "\n\t\tQuestion: " + rs.getString("question"));
                        }
                        break;
                    default:
                        run = false;
                        break;
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void insert(Scanner input, String query, int chapterId, int subChapterId) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, input.nextLine());
        if (chapterId != 0)
            pstmt.setInt(2, chapterId);
        if (subChapterId != 0)
            pstmt.setInt(3, subChapterId);
        pstmt.executeUpdate();
    }

    private static Integer listItemsToChoose(String tableName, ResultSet rs, Scanner input) throws SQLException {
        System.out.println("Select a " + tableName + " to link:");
        Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        rs = stmt.executeQuery("SELECT Name, Id FROM " + tableName);
        System.out.println("0. - None");
        while (rs.next()) {
            iRows++;
            System.out.println(iRows + ". - " + rs.getString(1));
        }
        iRows = 0;

        rs.absolute(Integer.parseInt(input.nextLine()));
        return rs.getInt(2);
    }

    public static void invokeConnection() {
        if (connection == null)
            connection = DatabaseConnection.connect("jdbc:sqlserver://URMANICR-DH4241:1433;databaseName=Quiz;encrypt=false", "testDB", "testDB");
    }

    public static ResultSet getAllQuestions() throws SQLException{
        invokeConnection();
        Statement selectAll = connection.createStatement();
        rs = selectAll.executeQuery("SELECT CH.name as chname, SU.name as suname, * FROM Chapter AS CH WITH(nolock)"
                + " INNER JOIN Subchapter AS SU WITH(nolock)"
                + " ON CH.Id = SU.ChapterId"
                + " INNER JOIN question AS QU"
                + " ON QU.ChapterId = CH.Id AND QU.SubchapterId = SU.Id");
        return rs;
    }
}
