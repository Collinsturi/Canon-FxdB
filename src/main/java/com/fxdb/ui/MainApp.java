package com.fxdb.ui;

import com.fxdb.ui.onboarding.OnboardingPreferences;
import com.fxdb.ui.onboarding.OnboardingTour;
import com.fxdb.ui.onboarding.TourStep;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    private final OnboardingPreferences onboardingPreferences = new OnboardingPreferences();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Canon FxdB - Database Migration Tool");
        SplashScreen.show(() -> {
            Navigator navigator = new Navigator(primaryStage);
            navigator.showConnectionSetup();
            if (!onboardingPreferences.hasSeenTour()) {
                OnboardingTour.show(primaryStage, TourStep.wizardOverview(), onboardingPreferences::markTourSeen);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
