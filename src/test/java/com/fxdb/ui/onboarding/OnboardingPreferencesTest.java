package com.fxdb.ui.onboarding;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingPreferencesTest {

    // A node under this test class, not the real OnboardingPreferences node, so this test
    // never touches (or is affected by) whatever the real app has actually recorded.
    private final Preferences testNode = Preferences.userNodeForPackage(OnboardingPreferencesTest.class);
    private final OnboardingPreferences preferences = new OnboardingPreferences(testNode);

    @AfterEach
    void clearTestNode() throws Exception {
        testNode.clear();
    }

    @Test
    void defaultsToNotHavingSeenTheTour() {
        assertThat(preferences.hasSeenTour()).isFalse();
    }

    @Test
    void remembersThatTheTourWasSeen() {
        preferences.markTourSeen();
        assertThat(preferences.hasSeenTour()).isTrue();
    }
}
