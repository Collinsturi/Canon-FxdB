package com.fxdb.transfer;

import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.ColumnTransform;
import com.fxdb.mapping.model.TableMapping;

import java.util.ArrayList;
import java.util.List;

/** Applies a TableMapping's column renames and transforms to every row in a batch. */
public final class RowConverter {

    private RowConverter() {
    }

    public static RowBatch convert(RowBatch batch, TableMapping mapping) {
        List<Row> converted = new ArrayList<>(batch.size());
        for (Row row : batch.rows()) {
            Row out = new Row();
            for (ColumnMapping columnMapping : mapping.getColumns()) {
                if (!columnMapping.isIncluded() || columnMapping.getTargetColumn() == null) {
                    continue;
                }
                Object value = row.get(columnMapping.getSourceColumn());
                out = out.with(columnMapping.getTargetColumn(), applyTransform(value, columnMapping.getTransform()));
            }
            converted.add(out);
        }
        return new RowBatch(converted);
    }

    private static Object applyTransform(Object value, ColumnTransform transform) {
        if (value == null) {
            return null;
        }
        return switch (transform) {
            case TRIM -> value instanceof String s ? s.trim() : value;
            // DATE_FORMAT, CAST and CUSTOM_EXPRESSION are reserved for the mapping grid (Phase 4)
            // and currently pass the value through unchanged.
            case NONE, DATE_FORMAT, CAST, CUSTOM_EXPRESSION -> value;
        };
    }
}
