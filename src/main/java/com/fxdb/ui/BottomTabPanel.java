package com.fxdb.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bottom-docked, browser-tab-styled strip - tabs cluster in the bottom-left corner, clicking
 * one opens its content above the strip. A collapse arrow in the top-right corner of the open
 * content (or clicking the tab again) closes it (the tab strip itself never disappears).
 * Generic on purpose, no knowledge of what any tab holds, so any screen can register its own
 * tab here (the query log today, an "Errors" tab during a migration run, or anything else later).
 */
public class BottomTabPanel extends BorderPane {

    private static final String TAB_INACTIVE_STYLE =
            "-fx-background-color: #e0e0e0; -fx-background-radius: 6 6 0 0; "
                    + "-fx-padding: 4 14 4 14; -fx-font-size: 11px; -fx-border-width: 0;";
    private static final String TAB_ACTIVE_STYLE =
            "-fx-background-color: white; -fx-background-radius: 6 6 0 0; "
                    + "-fx-padding: 4 14 4 14; -fx-font-size: 11px; -fx-font-weight: bold; "
                    + "-fx-border-color: #2d7dd2 transparent transparent transparent; -fx-border-width: 2 0 0 0;";
    private static final String TOP_EDGE_BORDER = "-fx-border-color: #999999; -fx-border-width: 2 0 0 0;";
    private static final String COLLAPSE_ARROW_STYLE =
            "-fx-background-color: #e8e8e8; -fx-background-radius: 4; -fx-font-size: 15px; "
                    + "-fx-padding: 2 10 2 10; -fx-text-fill: #444444;";

    private final Map<String, Node> contents = new LinkedHashMap<>();
    private final Map<String, Button> tabButtons = new LinkedHashMap<>();
    private final Set<String> pinned = new HashSet<>();
    private final HBox tabBar = new HBox(2);
    private final Button collapseArrow = new Button("▾"); // ▾
    private final BorderPane contentHeader = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final BorderPane contentHost = new BorderPane();

    private String activeTab;

    public BottomTabPanel() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(4, 8, 4, 8));
        tabBar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        collapseArrow.setStyle(COLLAPSE_ARROW_STYLE);
        collapseArrow.setFocusTraversable(false);
        collapseArrow.setOnAction(e -> {
            if (activeTab != null && !pinned.contains(activeTab)) {
                collapse();
            }
        });

        // A thin header above the actual tab content, holding just the collapse arrow in its
        // top-right corner - big enough to be an obvious click target without eating into the
        // content area itself.
        contentHeader.setRight(collapseArrow);
        contentHeader.setPadding(new Insets(6, 8, 6, 8));
        contentHeader.setStyle("-fx-background-color: #fafafa;" + TOP_EDGE_BORDER);

        contentHost.setTop(contentHeader);
        contentHost.setCenter(contentArea);
        contentHost.setManaged(false);
        contentHost.setVisible(false);

        setCenter(contentHost);
        setBottom(tabBar);
    }

    public void addTab(String title, Node content) {
        contents.put(title, content);

        Button button = new Button(title);
        button.setStyle(TAB_INACTIVE_STYLE);
        button.setOnAction(e -> {
            if (title.equals(activeTab)) {
                // Pinned means "can't be collapsed while active" - not "can't be switched to/from,"
                // so a pinned tab stays fully reachable from any other tab the whole time.
                if (!pinned.contains(title)) {
                    collapse();
                }
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
        contentHost.setManaged(true);
        contentHost.setVisible(true);
        tabButtons.forEach((t, b) -> b.setStyle(t.equals(title) ? TAB_ACTIVE_STYLE : TAB_INACTIVE_STYLE));
        collapseArrow.setDisable(pinned.contains(title));
    }

    public void collapse() {
        activeTab = null;
        contentHost.setManaged(false);
        contentHost.setVisible(false);
        tabButtons.values().forEach(b -> b.setStyle(TAB_INACTIVE_STYLE));
    }

    /** True when {@code title} is the tab currently open/showing - i.e. the user is looking at it. */
    public boolean isTabActive(String title) {
        return title.equals(activeTab);
    }

    /**
     * While pinned, clicking {@code title} when it's already the active tab (or the collapse
     * arrow, while it's the active one) does nothing - it can't be collapsed - but it stays
     * fully clickable to switch to from any other tab, and other tabs stay reachable from it
     * too. Use this instead of disabling the button outright, which would also block switching
     * back to it later.
     */
    public void setTabPinned(String title, boolean pinned) {
        if (pinned) {
            this.pinned.add(title);
        } else {
            this.pinned.remove(title);
        }
        if (title.equals(activeTab)) {
            collapseArrow.setDisable(pinned);
        }
    }
}
