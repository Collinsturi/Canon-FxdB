package com.fxdb.ui;

import com.fxdb.core.QueryLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The "what is fxDb actually telling the database" content - hosted as a tab inside a
 * {@link BottomTabPanel} rather than owning any chrome of its own (the tab button is its
 * label). Each line is tagged with which side of the migration it ran against (source =
 * yellow, target = green) so you can tell at a glance without reading the URL. Fed by
 * {@link QueryLog}; the caller (Navigator) is responsible for registering a listener that
 * marshals onto the FX thread before calling {@link #append}.
 */
public class QueryLogView extends ListView<QueryLog.Entry> {

    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String SOURCE_TAG_STYLE =
            "-fx-background-color: #f9e79f; -fx-text-fill: #7d6608; -fx-background-radius: 10; "
                    + "-fx-padding: 1 8 1 8; -fx-font-size: 10px; -fx-font-weight: bold;";
    private static final String TARGET_TAG_STYLE =
            "-fx-background-color: #a9dfbf; -fx-text-fill: #145a32; -fx-background-radius: 10; "
                    + "-fx-padding: 1 8 1 8; -fx-font-size: 10px; -fx-font-weight: bold;";

    private final ObservableList<QueryLog.Entry> entries = FXCollections.observableArrayList();

    public QueryLogView() {
        setItems(entries);
        setPrefHeight(140);
        setCellFactory(list -> new EntryCell());
    }

    public void append(QueryLog.Entry entry) {
        entries.add(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        scrollTo(entries.size() - 1);
    }

    private static final class EntryCell extends ListCell<QueryLog.Entry> {
        private final Label tag = new Label();
        private final Label text = new Label();
        private final HBox row = new HBox(8, tag, text);

        EntryCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(1, 0, 1, 0));
            text.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        }

        @Override
        protected void updateItem(QueryLog.Entry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                return;
            }
            boolean isSource = entry.role() == QueryLog.Role.SOURCE;
            tag.setText(isSource ? "SOURCE" : "TARGET");
            tag.setStyle(isSource ? SOURCE_TAG_STYLE : TARGET_TAG_STYLE);
            String time = entry.timestamp().atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
            text.setText("[" + time + "] " + entry.jdbcUrl() + ": " + entry.sql());
            setGraphic(row);
        }
    }
}
