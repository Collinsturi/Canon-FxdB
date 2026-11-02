package com.fxdb.file.csv;

import com.fxdb.core.DataSink;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CsvDataSink implements DataSink {

    private final FileConnectionConfig config;

    private TableSchema schema;
    private BufferedWriter writer;
    private CSVPrinter printer;

    public CsvDataSink(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open(TableSchema targetSchema, boolean createIfMissing) {
        this.schema = targetSchema;
        try {
            if (config.path().getParent() != null) {
                Files.createDirectories(config.path().getParent());
            }
            writer = Files.newBufferedWriter(config.path());
            List<String> headerNames = schema.columns().stream().map(Column::name).toList();
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(config.delimiter())
                    .setHeader(headerNames.toArray(new String[0]))
                    .build();
            printer = new CSVPrinter(writer, format);
        } catch (IOException e) {
            throw new TransferException("Failed to open CSV file for writing: " + config.path(), e);
        }
    }

    @Override
    public void writeBatch(RowBatch batch) {
        try {
            for (Row row : batch.rows()) {
                List<Object> values = new ArrayList<>(schema.columns().size());
                for (Column column : schema.columns()) {
                    values.add(row.get(column.name()));
                }
                printer.printRecord(values);
            }
        } catch (IOException e) {
            throw new TransferException("Failed to write CSV batch to " + config.path(), e);
        }
    }

    @Override
    public void commit() {
        try {
            printer.flush();
        } catch (IOException e) {
            throw new TransferException("Failed to flush CSV file " + config.path(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (printer != null) {
                printer.close();
            }
        } catch (IOException e) {
            throw new TransferException("Failed to close CSV file " + config.path(), e);
        }
    }
}
