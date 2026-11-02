package com.fxdb.file;

import com.fxdb.core.SchemaProvider;
import com.fxdb.core.exception.SchemaException;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.csv.CsvSchemaInferer;
import com.fxdb.file.excel.ExcelSchemaInferer;
import com.fxdb.file.json.JsonSchemaInferer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Discovers the "tables" a flat file exposes: a single pseudo-table named after the file
 * for CSV/JSON, or one table per sheet for Excel - so the UI can list objects the same way
 * for a file as it would for a JDBC connection.
 */
public class FileSchemaProvider implements SchemaProvider {

    private final FileConnectionConfig config;

    public FileSchemaProvider(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public List<String> listTables() {
        if (!Files.exists(config.path())) {
            // A target file that doesn't exist yet has no existing "tables" to match against -
            // MappingEngine should treat every selected source table as going into a new one.
            return List.of();
        }
        return switch (config.format()) {
            case CSV, JSON -> List.of(config.fileBaseName());
            case EXCEL -> listExcelSheetNames();
        };
    }

    private List<String> listExcelSheetNames() {
        try (Workbook workbook = WorkbookFactory.create(config.path().toFile())) {
            List<String> names = new java.util.ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
            return names;
        } catch (IOException e) {
            throw new SchemaException("Failed to list sheets in " + config.path(), e);
        }
    }

    @Override
    public TableSchema describeTable(String tableName) {
        return switch (config.format()) {
            case CSV -> CsvSchemaInferer.infer(config);
            case JSON -> JsonSchemaInferer.infer(config);
            case EXCEL -> ExcelSchemaInferer.infer(
                    new FileConnectionConfig(config.path(), config.format(), config.delimiter(),
                            config.hasHeader(), tableName));
        };
    }

    @Override
    public void close() {
        // nothing held open between calls; each inference opens and closes its own file handle
    }
}
