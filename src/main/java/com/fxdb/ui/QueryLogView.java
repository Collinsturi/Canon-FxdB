package com.fxdb.ui;

import com.fxdb.core.QueryLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The "what is fxDb actually telling the database" content - a plain ListView, hosted as a tab
 * inside a {@link BottomTabPanel} rather than owning any chrome of its own (the tab button is
 * its label). Fed by {@link QueryLog}; the caller (Navigator) is responsible for registering a
 * listener that marshals onto the FX thread before calling {@link #append}.
 */
public class QueryLogView extends ListView<String> {

    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> lines = FXCollections.observableArrayList();

    public QueryLogView() {
        setItems(lines);
        setPrefHeight(140);
        setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
    }

    public void append(QueryLog.Entry entry) {
        String time = entry.timestamp().atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
        lines.add("[" + time + "] " + entry.jdbcUrl() + ": " + entry.sql());
        while (lines.size() > MAX_ENTRIES) {
            lines.remove(0);
        }
        scrollTo(lines.size() - 1);
    }
}
