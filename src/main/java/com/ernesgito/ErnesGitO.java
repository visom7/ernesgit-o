package com.ernesgito;

import com.ernesgito.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * ErnesGit-O — Your personal Git management tool.
 * JavaFX application entry point.
 */
public class ErnesGitO extends Application {

    public static final String VERSION = "1.0.0";

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow(stage);

        Scene scene = new Scene(window.getRoot(), 1280, 820);
        scene.getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm()
        );

        stage.setTitle("ErnesGit-O  v" + VERSION);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
