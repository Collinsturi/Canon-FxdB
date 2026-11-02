package com.fxdb.file.excel;

import com.fxdb.core.DataSource;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Reads an Excel sheet via plain POI row iteration. For MVP scope this loads the whole
 * workbook into memory (acceptable for a manually-run, one-shot migration); very large
 * spreadsheets are a known limitation documented in the project plan, to revisit with the
 * streaming XSSFReader/SAX API if needed later.
 */
public class ExcelDataSource implements DataSource {

    private final FileConnectionConfig config;

    private Workbook workbook;
    private Sheet sheet;
    private TableSchema schema;
    private int nextRow;
    private int lastRow;

    public ExcelDataSource(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open() {
        try {
            workbook = WorkbookFactory.create(config.path().toFile());
        } catch (IOException e) {
            throw new TransferException("Failed to open Excel file " + config.path(), e);
        }
        sheet = ExcelSchemaInferer.resolveSheet(workbook, config.sheetName());
        schema = ExcelSchemaInferer.infer(sheet, config.fileBaseName());
        nextRow = sheet.getFirstRowNum() + 1;
        lastRow = sheet.getLastRowNum();
    }

    @Override
    public TableSchema getSchema() {
        return schema;
    }

    @Override
    public long estimateRowCount() {
        return Math.max(lastRow - sheet.getFirstRowNum(), 0);
    }

    @Override
    public Iterator<RowBatch> readBatches(int batchSize) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return nextRow <= lastRow;
            }

            @Override
            public RowBatch next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                List<com.fxdb.core.model.Row> rows = new ArrayList<>(batchSize);
                while (rows.size() < batchSize && nextRow <= lastRow) {
                    var poiRow = sheet.getRow(nextRow++);
                    rows.add(toRow(poiRow));
                }
                return new RowBatch(rows);
            }
        };
    }

    private com.fxdb.core.model.Row toRow(org.apache.poi.ss.usermodel.Row poiRow) {
        com.fxdb.core.model.Row row = new com.fxdb.core.model.Row();
        List<Column> columns = schema.columns();
        for (int c = 0; c < columns.size(); c++) {
            Column column = columns.get(c);
            Object value = poiRow == null ? null : ExcelCellAccess.cellValue(poiRow.getCell(c), column.type());
            row = row.with(column.name(), value);
        }
        return row;
    }

    @Override
    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
            }
        } catch (IOException e) {
            throw new TransferException("Failed to close Excel file " + config.path(), e);
        }
    }
}
