package org.example;

import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.example.Application.AppUI;
import org.example.Application.Application;
import org.example.database.DatabaseConnection;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {


    public static void main(String[] args) {
        AppUI appUi = new AppUI();
        appUi.launch(AppUI.class, args);
        // Application.run();
    }
}