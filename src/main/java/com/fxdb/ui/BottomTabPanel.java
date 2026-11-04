package com.fxdb.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bottom-docked, browser-tab-styled strip - tabs cluster in the bottom-left corner, clicking
 * one opens its content above the strip, clicking the already-open one collapses it again (the
 * strip itself never disappears). Generic on purpose, no knowledge of what any tab holds, so any
 * screen can register its own tab here (the query log today, an "Errors" tab during a migration
 * run, or anything else later).
 */
public class BottomTabPanel extends BorderPane {

    private static final String TAB_INACTIVE_STYLE =
            "-fx-background-color: #e0e0e0; -fx-background-radius: 6 6 0 0; "
                    + "-fx-padding: 4 14 4 14; -fx-font-size: 11px; -fx-border-width: 0;";
    private static final String TAB_ACTIVE_STYLE =
            "-fx-background-color: white; -fx-background-radius: 6 6 0 0; "
                    + "-fx-padding: 4 14 4 14; -fx-font-size: 11px; -fx-font-weight: bold; "
                    + "-fx-border-color: #2d7dd2 transparent transparent transparent; -fx-border-width: 2 0 0 0;";

    private final Map<String, Node> contents = new LinkedHashMap<>();
    private final Map<String, Button> tabButtons = new LinkedHashMap<>();
    private final HBox tabBar = new HBox(2);
    private final StackPane contentArea = new StackPane();

    private String activeTab;

    public BottomTabPanel() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(4, 8, 0, 8));
        tabBar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        contentArea.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        contentArea.setManaged(false);
        contentArea.setVisible(false);

        setCenter(contentArea);
        setBottom(tabBar);
    }

    public void addTab(String title, Node content) {
        contents.put(title, content);

        Button button = new Button(title);
        button.setStyle(TAB_INACTIVE_STYLE);
        button.setOnAction(e -> {
            if (title.equals(activeTab)) {
                collapse();
            } else {
                openTab(title);
            }
        });
        tabButtons.put(title, button);
        tabBar.getChildren().add(button);
    }

    public void openTab(String title) {
        activeTab = title;
        contentArea.getChildren().setAll(contents.get(title));
        contentArea.setManaged(true);
        contentArea.setVisible(true);
        tabButtons.forEach((t, b) -> b.setStyle(t.equals(title) ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE));
    }

    public void collapse() {
        activeTab = null;
        contentArea.setManaged(false);
        contentArea.setVisible(false);
        tabButtons.values().forEach(b -> b.setStyle(TAB_INACTIVE_STYLE));
    }

    /** True when {@code title} is the tab currently open/showing - i.e. the user is looking at it. */
    public boolean isTabActive(String title) {
        return title.equals(activeTab);
    }

    public void setTabDisabled(String title, boolean disabled) {
        Button button = tabButtons.get(title);
        if (button != null) {
            button.setDisable(disabled);
        }
    }
}
