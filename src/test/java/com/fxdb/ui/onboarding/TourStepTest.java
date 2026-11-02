package com.fxdb.ui.onboarding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourStepTest {

    @Test
    void coversAllFiveWizardScreensWithNonEmptyContent() {
        var steps = TourStep.wizardOverview();

        assertThat(steps).hasSize(5);
        assertThat(steps).allSatisfy(step -> {
            assertThat(step.title()).isNotBlank();
            assertThat(step.description()).isNotBlank();
        });
    }
}
