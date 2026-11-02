package com.fxdb.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record Row(Map<String, Object> values) {
    public Row() {
        this(new LinkedHashMap<>());
    }

    public Object get(String column) {
        return values.get(column);
    }

    public Row with(String column, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(column, value);
        return new Row(copy);
    }
}
