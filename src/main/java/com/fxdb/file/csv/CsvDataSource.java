package com.fxdb.file.csv;

import com.fxdb.core.DataSource;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.CellValueParser;
import com.fxdb.file.FileConnectionConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CsvDataSource implements DataSource {

    private final FileConnectionConfig config;

    private TableSchema schema;
    private BufferedReader reader;
    private CSVParser parser;

    public CsvDataSource(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open() {
        schema = CsvSchemaInferer.infer(config);
        try {
            reader = Files.newBufferedReader(config.path());
            parser = CsvSchemaInferer.csvFormat(config).parse(reader);
        } catch (IOException e) {
            throw new TransferException("Failed to open CSV file " + config.path(), e);
        }
    }

    @Override
    public TableSchema getSchema() {
        return schema;
    }

    @Override
    public long estimateRowCount() {
        try (BufferedReader counter = Files.newBufferedReader(config.path())) {
            long lines = counter.lines().count();
            return config.hasHeader() ? Math.max(lines - 1, 0) : lines;
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public Iterator<RowBatch> readBatches(int batchSize) {
        Iterator<CSVRecord> records = parser.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return records.hasNext();
            }

            @Override
            public RowBatch next() {
                if (!records.hasNext()) {
                    throw new NoSuchElementException();
                }
                List<Row> rows = new ArrayList<>(batchSize);
                while (rows.size() < batchSize && records.hasNext()) {
                    rows.add(toRow(records.next()));
                }
                return new RowBatch(rows);
            }
        };
    }

    private Row toRow(CSVRecord record) {
        Row row = new Row();
        for (var column : schema.columns()) {
            String raw = record.isSet(column.name()) ? record.get(column.name()) : null;
            row = row.with(column.name(), CellValueParser.parse(raw, column.type()));
        }
        return row;
    }

    @Override
    public void close() {
        try {
            if (parser != null) {
                parser.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
