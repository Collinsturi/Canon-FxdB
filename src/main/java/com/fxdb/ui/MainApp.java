package com.fxdb.ui;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("fxDb - Database Migration Tool");
        Navigator navigator = new Navigator(primaryStage);
        navigator.showConnectionSetup();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
