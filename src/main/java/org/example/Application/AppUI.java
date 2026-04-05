package org.example.Application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppUI extends javafx.application.Application {
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
                    displayArea.setText(displayArea.getText() + "Question: " + rs.getString("question") + "\n");
                    inputField.setText(inputField.getText() + "Question: " + rs.getString("question") + "\n");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        StackPane root = new StackPane();
//        root.getChildren().add(btn);

        inputField.setPromptText("prompt text");
//        root.getChildren().add(inputField);

        ComboBox<String> mainCategory = new ComboBox<>();
        ComboBox<String> subCategory = new ComboBox<>();

        mainCategory.setPromptText("Select Category");
        mainCategory.getItems().addAll("Programming", "Databases");

// 2. Define the data for the second dropdown
        ObservableList<String> programmingLangs = FXCollections.observableArrayList("Java", "Python", "C#");
        ObservableList<String> databaseTypes = FXCollections.observableArrayList("MySQL", "PostgreSQL", "MongoDB");

// 3. Add the Listener to change values dynamically
        mainCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals("Programming")) {
                subCategory.setItems(programmingLangs);
            } else if (newVal.equals("Databases")) {
                subCategory.setItems(databaseTypes);
            }
            subCategory.getSelectionModel().selectFirst(); // Optional: select first item automatically
        });

// 4. Style them to be "larger"
        mainCategory.setPrefWidth(200);
        mainCategory.setPrefHeight(40);
        subCategory.setPrefWidth(200);
        subCategory.setPrefHeight(40);

// 5. Put them in a container at the top
        HBox topBar = new HBox(15, mainCategory, subCategory);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new javafx.geometry.Insets(20));


        VBox layout = new VBox(10);
        layout.getChildren().addAll(topBar, inputField, btn, displayArea);

        Scene scene = new Scene(layout, 700, 625);

        primaryStage.setTitle("JavaFX Test Run");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
