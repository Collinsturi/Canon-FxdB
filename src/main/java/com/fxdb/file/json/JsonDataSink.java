package com.fxdb.file.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fxdb.core.DataSink;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;

public class JsonDataSink implements DataSink {

    private static final JsonFactory FACTORY = new JsonFactory();

    private final FileConnectionConfig config;

    private TableSchema schema;
    private JsonGenerator generator;

    public JsonDataSink(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open(TableSchema targetSchema, boolean createIfMissing) {
        this.schema = targetSchema;
        try {
            if (config.path().getParent() != null) {
                Files.createDirectories(config.path().getParent());
            }
            generator = FACTORY.createGenerator(Files.newBufferedWriter(config.path()));
            generator.writeStartArray();
        } catch (IOException e) {
            throw new TransferException("Failed to open JSON file for writing: " + config.path(), e);
        }
    }

    @Override
    public void writeBatch(RowBatch batch) {
        try {
            for (Row row : batch.rows()) {
                generator.writeStartObject();
                for (Column column : schema.columns()) {
                    writeField(column.name(), row.get(column.name()));
                }
                generator.writeEndObject();
            }
        } catch (IOException e) {
            throw new TransferException("Failed to write JSON batch to " + config.path(), e);
        }
    }

    private void writeField(String name, Object value) throws IOException {
        if (value == null) {
            generator.writeNullField(name);
        } else if (value instanceof Long l) {
            generator.writeNumberField(name, l);
        } else if (value instanceof Integer i) {
            generator.writeNumberField(name, i);
        } else if (value instanceof Double d) {
            generator.writeNumberField(name, d);
        } else if (value instanceof BigDecimal bd) {
            generator.writeNumberField(name, bd);
        } else if (value instanceof Boolean b) {
            generator.writeBooleanField(name, b);
        } else {
            generator.writeStringField(name, value.toString());
        }
    }

    @Override
    public void commit() {
        try {
            generator.flush();
        } catch (IOException e) {
            throw new TransferException("Failed to flush JSON file " + config.path(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (generator != null) {
                generator.writeEndArray();
                generator.close();
            }
        } catch (IOException e) {
            throw new TransferException("Failed to close JSON file " + config.path(), e);
        }
    }
}
