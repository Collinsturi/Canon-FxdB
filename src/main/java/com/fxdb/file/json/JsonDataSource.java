package com.fxdb.file.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fxdb.core.DataSource;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class JsonDataSource implements DataSource {

    private static final JsonFactory FACTORY = new JsonFactory();

    private final FileConnectionConfig config;

    private TableSchema schema;
    private JsonParser parser;

    public JsonDataSource(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open() {
        schema = JsonSchemaInferer.infer(config);
        try {
            parser = FACTORY.createParser(config.path().toFile());
            parser.nextToken(); // START_ARRAY
            parser.nextToken(); // first START_OBJECT, or END_ARRAY if empty
        } catch (IOException e) {
            throw new TransferException("Failed to open JSON file " + config.path(), e);
        }
    }

    @Override
    public TableSchema getSchema() {
        return schema;
    }

    @Override
    public long estimateRowCount() {
        return -1;
    }

    @Override
    public Iterator<RowBatch> readBatches(int batchSize) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return parser.currentToken() == JsonToken.START_OBJECT;
            }

            @Override
            public RowBatch next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                List<Row> rows = new ArrayList<>(batchSize);
                try {
                    while (rows.size() < batchSize && hasNext()) {
                        rows.add(readObject());
                    }
                } catch (IOException e) {
                    throw new TransferException("Failed to read JSON batch from " + config.path(), e);
                }
                return new RowBatch(rows);
            }
        };
    }

    private Row readObject() throws IOException {
        Row row = new Row();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();
            row = row.with(fieldName, JsonValueCodec.readScalar(parser, fieldName));
        }
        parser.nextToken(); // advance to next START_OBJECT or END_ARRAY
        return row;
    }

    @Override
    public void close() {
        try {
            if (parser != null) {
                parser.close();
            }
        } catch (IOException e) {
            throw new TransferException("Failed to close JSON file " + config.path(), e);
        }
    }
}
