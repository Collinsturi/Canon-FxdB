package com.fxdb.file;

import com.fxdb.core.model.DataType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Infers a unified DataType for a column from a sample of raw string cell values, shared by
 * the CSV and Excel schema inferers. Widens progressively: an all-integer column stays
 * INTEGER/LONG, mixing in a decimal widens to DOUBLE, mixing in anything non-numeric falls
 * back to STRING.
 */
public final class ValueTypeInferer {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE
    };
    private static final DateTimeFormatter[] TIMESTAMP_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    private ValueTypeInferer() {
    }

    public static DataType inferColumnType(List<String> sampleValues) {
        DataType running = null;
        for (String raw : sampleValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            DataType valueType = classify(raw);
            running = running == null ? valueType : widen(running, valueType);
        }
        return running == null ? DataType.STRING : running;
    }

    public static boolean anyBlank(List<String> sampleValues) {
        return sampleValues.stream().anyMatch(v -> v == null || v.isBlank());
    }

    private static DataType classify(String raw) {
        if (tryParseLong(raw)) return DataType.LONG;
        if (tryParseDouble(raw)) return DataType.DOUBLE;
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) return DataType.BOOLEAN;
        if (tryParse(raw, DATE_FORMATS, LocalDate::parse)) return DataType.DATE;
        if (tryParse(raw, TIMESTAMP_FORMATS, LocalDateTime::parse)) return DataType.TIMESTAMP;
        return DataType.STRING;
    }

    private static DataType widen(DataType a, DataType b) {
        if (a == b) return a;
        if (isNumeric(a) && isNumeric(b)) return DataType.DOUBLE;
        if (isTemporal(a) && isTemporal(b)) return DataType.TIMESTAMP;
        return DataType.STRING;
    }

    private static boolean isNumeric(DataType type) {
        return type == DataType.LONG || type == DataType.DOUBLE || type == DataType.INTEGER;
    }

    private static boolean isTemporal(DataType type) {
        return type == DataType.DATE || type == DataType.TIMESTAMP;
    }

    private static boolean tryParseLong(String raw) {
        try {
            Long.parseLong(raw);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean tryParseDouble(String raw) {
        try {
            Double.parseDouble(raw);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean tryParse(String raw, DateTimeFormatter[] formats, java.util.function.BiFunction<CharSequence, DateTimeFormatter, ?> parser) {
        for (DateTimeFormatter formatter : formats) {
            try {
                parser.apply(raw, formatter);
                return true;
            } catch (Exception ignored) {
                // try next format
            }
        }
        return false;
    }
}
