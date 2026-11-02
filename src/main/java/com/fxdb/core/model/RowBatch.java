package com.fxdb.core.model;

import java.util.List;

public record RowBatch(List<Row> rows) {
    public int size() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
