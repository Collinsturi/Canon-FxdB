package com.fxdb.file.excel;

import com.fxdb.core.exception.SchemaException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.DataType;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Infers a TableSchema from an Excel sheet's header row plus a sample of the following rows. */
public final class ExcelSchemaInferer {

    private static final int SAMPLE_SIZE = 100;

    private ExcelSchemaInferer() {
    }

    public static TableSchema infer(FileConnectionConfig config) {
        try (Workbook workbook = WorkbookFactory.create(config.path().toFile())) {
            Sheet sheet = resolveSheet(workbook, config.sheetName());
            return infer(sheet, config.fileBaseName());
        } catch (IOException e) {
            throw new SchemaException("Failed to infer schema from Excel file " + config.path(), e);
        }
    }

    static TableSchema infer(Sheet sheet, String tableName) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new SchemaException("Sheet '" + sheet.getSheetName() + "' has no header row");
        }

        List<String> headers = new ArrayList<>();
        headerRow.forEach(cell -> headers.add(cell.getStringCellValue()));

        Map<String, DataType> columnTypes = new LinkedHashMap<>();
        Map<String, Boolean> nullable = new LinkedHashMap<>();
        headers.forEach(h -> {
            columnTypes.put(h, null);
            nullable.put(h, false);
        });

        int firstDataRow = sheet.getFirstRowNum() + 1;
        int lastSampleRow = Math.min(sheet.getLastRowNum(), firstDataRow + SAMPLE_SIZE - 1);
        for (int r = firstDataRow; r <= lastSampleRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = 0; c < headers.size(); c++) {
                String header = headers.get(c);
                DataType cellType = ExcelCellAccess.cellDataType(row.getCell(c));
                if (cellType == null) {
                    nullable.put(header, true);
                } else {
                    columnTypes.merge(header, cellType, ExcelSchemaInferer::widen);
                }
            }
        }

        List<Column> columns = headers.stream()
                .map(h -> new Column(h,
                        columnTypes.get(h) == null ? DataType.STRING : columnTypes.get(h),
                        nullable.get(h)))
                .toList();

        return new TableSchema(null, null, tableName, columns, List.of(), List.of());
    }

    static Sheet resolveSheet(Workbook workbook, String sheetName) {
        Sheet sheet = sheetName != null ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
        if (sheet == null) {
            throw new SchemaException("Sheet '" + sheetName + "' not found");
        }
        return sheet;
    }

    private static DataType widen(DataType a, DataType b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a == b) return a;
        boolean aNumeric = a == DataType.LONG || a == DataType.DOUBLE;
        boolean bNumeric = b == DataType.LONG || b == DataType.DOUBLE;
        if (aNumeric && bNumeric) return DataType.DOUBLE;
        boolean aTemporal = a == DataType.DATE || a == DataType.TIMESTAMP;
        boolean bTemporal = b == DataType.DATE || b == DataType.TIMESTAMP;
        if (aTemporal && bTemporal) return DataType.TIMESTAMP;
        return DataType.STRING;
    }
}
