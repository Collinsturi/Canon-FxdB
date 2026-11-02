package com.fxdb.file.excel;

import com.fxdb.core.model.DataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

/** Shared cell-type detection and value extraction used by both the Excel inferer and reader. */
final class ExcelCellAccess {

    private ExcelCellAccess() {
    }

    static DataType cellDataType(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case BLANK -> null;
            case BOOLEAN -> DataType.BOOLEAN;
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? (hasTimeComponent(cell.getNumericCellValue()) ? DataType.TIMESTAMP : DataType.DATE)
                    : (isWholeNumber(cell.getNumericCellValue()) ? DataType.LONG : DataType.DOUBLE);
            case STRING -> DataType.STRING;
            default -> DataType.STRING; // formulas and errors: read back as text, kept simple for MVP
        };
    }

    static Object cellValue(Cell cell, DataType columnType) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return switch (columnType) {
            case LONG -> (long) cell.getNumericCellValue();
            case DOUBLE -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case DATE -> cell.getLocalDateTimeCellValue().toLocalDate();
            case TIMESTAMP -> cell.getLocalDateTimeCellValue();
            default -> cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : cell.toString();
        };
    }

    private static boolean isWholeNumber(double value) {
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    private static boolean hasTimeComponent(double excelDateValue) {
        return excelDateValue % 1.0 != 0.0;
    }
}
