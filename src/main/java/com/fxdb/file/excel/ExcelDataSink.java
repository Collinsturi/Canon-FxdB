package com.fxdb.file.excel;

import com.fxdb.core.DataSink;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ExcelDataSink implements DataSink {

    private final FileConnectionConfig config;

    private SXSSFWorkbook workbook;
    private Sheet sheet;
    private CellStyle dateStyle;
    private TableSchema schema;
    private int nextRow;
    private boolean written;

    public ExcelDataSink(FileConnectionConfig config) {
        this.config = config;
    }

    @Override
    public void open(TableSchema targetSchema, boolean createIfMissing) {
        this.schema = targetSchema;
        workbook = new SXSSFWorkbook(100);
        sheet = workbook.createSheet(config.sheetName() != null ? config.sheetName() : "Sheet1");

        CreationHelper creationHelper = workbook.getCreationHelper();
        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        List<Column> columns = schema.columns();
        var headerRow = sheet.createRow(0);
        for (int c = 0; c < columns.size(); c++) {
            headerRow.createCell(c).setCellValue(columns.get(c).name());
        }
        nextRow = 1;
    }

    @Override
    public void writeBatch(RowBatch batch) {
        List<Column> columns = schema.columns();
        for (Row row : batch.rows()) {
            var sheetRow = sheet.createRow(nextRow++);
            for (int c = 0; c < columns.size(); c++) {
                Column column = columns.get(c);
                setCellValue(sheetRow.createCell(c), row.get(column.name()));
            }
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }
        switch (value) {
            case Boolean b -> cell.setCellValue(b);
            case Number n -> cell.setCellValue(n.doubleValue());
            case LocalDate d -> {
                cell.setCellValue(d);
                cell.setCellStyle(dateStyle);
            }
            case LocalDateTime dt -> {
                cell.setCellValue(dt);
                cell.setCellStyle(dateStyle);
            }
            default -> cell.setCellValue(value.toString());
        }
    }

    @Override
    public void commit() {
        if (written) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(ensureParentExists())) {
            workbook.write(out);
            written = true;
        } catch (IOException e) {
            throw new TransferException("Failed to write Excel file " + config.path(), e);
        }
    }

    private java.io.File ensureParentExists() throws IOException {
        if (config.path().getParent() != null) {
            Files.createDirectories(config.path().getParent());
        }
        return config.path().toFile();
    }

    @Override
    public void close() {
        if (workbook != null) {
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException e) {
                throw new TransferException("Failed to close Excel workbook for " + config.path(), e);
            }
        }
    }
}
