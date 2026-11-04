package com.fxdb.ui;

import com.fxdb.core.QueryLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * The persistent "what is fxDb actually telling the database" panel - lives at the bottom of
 * Navigator's shell, so it survives every wizard screen swap the same way the step indicator
 * does. Fed by {@link QueryLog}; the caller (Navigator) is responsible for registering a
 * listener that marshals onto the FX thread before calling {@link #append}.
 */
public class QueryLogPanel extends VBox {

    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> lines = FXCollections.observableArrayList();
    private final ListView<String> listView = new ListView<>(lines);

    public QueryLogPanel() {
        Label header = new Label("Database Activity");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        listView.setPrefHeight(140);
        listView.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        setSpacing(4);
        setPadding(new Insets(6, 10, 6, 10));
        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        getChildren().addAll(header, listView);
    }

    public void append(QueryLog.Entry entry) {
        String time = entry.timestamp().atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
        lines.add("[" + time + "] " + entry.jdbcUrl() + ": " + entry.sql());
        while (lines.size() > MAX_ENTRIES) {
            lines.remove(0);
        }
        listView.scrollTo(lines.size() - 1);
    }
}
