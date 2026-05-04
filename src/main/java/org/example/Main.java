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
import org.example.util.HibernateUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
public class Main {


    public static void main(String[] args) {
        HibernateUtil.getSessionFactory();

        SpringApplication.run(Main.class, args);

//        AppUI appUi = new AppUI();
//        appUi.launch(AppUI.class, args);
        // Application.run();
    }
}