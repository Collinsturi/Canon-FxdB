package com.fxdb.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Migration-run error messages, hosted as a tab inside a {@link BottomTabPanel} - same shape as
 * {@link QueryLogView}, just fed by {@code Navigator.reportError} instead of {@code QueryLog}.
 */
public class ErrorLogView extends ListView<String> {

    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> lines = FXCollections.observableArrayList();

    public ErrorLogView() {
        setItems(lines);
        setPrefHeight(140);
        setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(empty ? "" : "-fx-text-fill: #c0392b;");
            }
        });
    }

    public void append(String message) {
        lines.add("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message);
        while (lines.size() > MAX_ENTRIES) {
            lines.remove(0);
        }
        scrollTo(lines.size() - 1);
    }

    public void clear() {
        lines.clear();
    }
}
