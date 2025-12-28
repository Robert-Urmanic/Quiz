package org.example;

import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.example.Application.Application;
import org.example.database.DatabaseConnection;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Main extends javafx.application.Application {
    private static ResultSet rs;
    @Override
    public void start(Stage primaryStage) {
        Button btn = new Button();
        TextField inputField = new TextField();

        TextArea displayArea = new TextArea();
        displayArea.setEditable(false); // This makes it read-only
        displayArea.setWrapText(true);  // Wraps text to next line

        btn.setText("Say !");
        btn.setOnAction(event -> {
            try {
                displayArea.setText("");
                rs = Application.getAllQuestions();
                while (rs.next()) {
                    displayArea.setText(displayArea.getText()+ "Question: " + rs.getString("question") + "\n");
                    inputField.setText(inputField.getText() + "Question: " + rs.getString("question") + "\n");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        StackPane root = new StackPane();
//        root.getChildren().add(btn);

        inputField.setPromptText("niggas be niggaing, bitches be bitching, jews be jewing");
//        root.getChildren().add(inputField);

        VBox layout = new VBox(10);
        layout.getChildren().addAll(inputField, btn, displayArea);

        Scene scene = new Scene(layout, 700, 625);

        primaryStage.setTitle("JavaFX Test Run");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
        // Application.run();
    }
}