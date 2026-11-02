package com.fxdb.ui.onboarding;

import java.util.List;

/** One slide of the onboarding tour: which screen it's about, and what you can do there. */
public record TourStep(String title, String description) {

    public static List<TourStep> wizardOverview() {
        return List.of(
                new TourStep(
                        "1. Connect source and target",
                        "Point each side at a database (Postgres, MySQL, SQL Server, Oracle, SQLite) "
                                + "or a file (CSV, JSON, Excel). Test Connection lists what it finds before "
                                + "you commit to anything."),
                new TourStep(
                        "2. Choose what to migrate",
                        "Pick which tables or sheets to bring over and what to call them on the "
                                + "other side. Everything you don't check stays untouched."),
                new TourStep(
                        "3. Review the mapping",
                        "Columns are auto-matched by name and type. Override anything that's wrong, "
                                + "exclude a column, or re-run Auto-match after changing a target. Save the "
                                + "mapping as a profile to reuse it next time."),
                new TourStep(
                        "4. Run it",
                        "Choose what happens if a row fails - abort, skip it, or retry first - then "
                                + "watch live progress per table. Cancel stops cleanly after the current batch."),
                new TourStep(
                        "5. Check the report",
                        "See exactly what moved: rows read/written/skipped per table, every error "
                                + "message, and an Export button to save the report as a text file.")
        );
    }
}
