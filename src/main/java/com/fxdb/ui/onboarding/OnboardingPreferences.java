package com.fxdb.ui.onboarding;

import java.util.prefs.Preferences;

/** Remembers whether the user has already seen the onboarding tour, across app launches. */
public class OnboardingPreferences {

    private static final String SEEN_TOUR_KEY = "onboarding.seenTour";

    private final Preferences preferences;

    public OnboardingPreferences() {
        this(Preferences.userNodeForPackage(OnboardingPreferences.class));
    }

    /** Package-visible so tests can point this at a throwaway node instead of the real user prefs. */
    OnboardingPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean hasSeenTour() {
        return preferences.getBoolean(SEEN_TOUR_KEY, false);
    }

    public void markTourSeen() {
        preferences.putBoolean(SEEN_TOUR_KEY, true);
    }
}
