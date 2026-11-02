package com.fxdb.ui;

/** Implemented by every screen controller so Navigator can hand it the shared wizard state after loading its FXML. */
public interface WizardScreen {
    void init(MigrationWizardContext context, Navigator navigator);
}
