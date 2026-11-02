package com.fxdb.file;

import com.fxdb.core.model.DataType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Converts a raw file cell string into the Java type matching a column's inferred DataType. */
public final class CellValueParser {

    private CellValueParser() {
    }

    public static Object parse(String raw, DataType type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return switch (type) {
                case LONG -> Long.parseLong(raw);
                case INTEGER -> Integer.parseInt(raw);
                case DOUBLE -> Double.parseDouble(raw);
                case DECIMAL -> new java.math.BigDecimal(raw);
                case BOOLEAN -> Boolean.parseBoolean(raw);
                case DATE -> LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
                case TIMESTAMP -> parseTimestamp(raw);
                default -> raw;
            };
        } catch (Exception e) {
            // Sample-based inference can still be wrong for a stray row; fall back to the raw string
            // rather than aborting the whole transfer over one malformed cell.
            return raw;
        }
    }

    private static LocalDateTime parseTimestamp(String raw) {
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
