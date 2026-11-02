package com.fxdb.file.csv;

import com.fxdb.core.exception.SchemaException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;
import com.fxdb.file.ValueTypeInferer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Infers a TableSchema from a CSV's header row plus a sample of the following rows. */
public final class CsvSchemaInferer {

    private static final int SAMPLE_SIZE = 100;

    private CsvSchemaInferer() {
    }

    public static TableSchema infer(FileConnectionConfig config) {
        CSVFormat format = csvFormat(config);
        try (Reader reader = Files.newBufferedReader(config.path());
             CSVParser parser = format.parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            Map<String, List<String>> samples = new LinkedHashMap<>();
            headers.forEach(h -> samples.put(h, new ArrayList<>()));

            int count = 0;
            for (CSVRecord record : parser) {
                for (String header : headers) {
                    samples.get(header).add(record.isSet(header) ? record.get(header) : null);
                }
                if (++count >= SAMPLE_SIZE) {
                    break;
                }
            }

            List<Column> columns = headers.stream()
                    .map(h -> new Column(h, ValueTypeInferer.inferColumnType(samples.get(h)),
                            ValueTypeInferer.anyBlank(samples.get(h))))
                    .toList();

            return new TableSchema(null, null, config.fileBaseName(), columns, List.of(), List.of());
        } catch (IOException e) {
            throw new SchemaException("Failed to infer schema from CSV file " + config.path(), e);
        }
    }

    static CSVFormat csvFormat(FileConnectionConfig config) {
        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setDelimiter(config.delimiter());
        if (config.hasHeader()) {
            builder.setHeader().setSkipHeaderRecord(true);
        }
        return builder.build();
    }
}
