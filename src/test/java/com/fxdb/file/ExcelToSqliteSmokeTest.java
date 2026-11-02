package com.fxdb.file;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.file.excel.ExcelDataSource;
import com.fxdb.transfer.TransferEngine;
import com.fxdb.transfer.TransferResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelToSqliteSmokeTest {

    @Test
    void migratesExcelSheetIntoSqlite(@TempDir Path tempDir) throws SQLException, IOException {
        Path excelFixture = tempDir.resolve("customers.xlsx");
        writeFixtureWorkbook(excelFixture);

        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        ExcelDataSource source = new ExcelDataSource(FileConnectionConfig.excel(excelFixture, "Customers"));
        JdbcDataSink sink = new JdbcDataSink(new JdbcConnectionConfig(targetUrl, null, null), "customers");

        TransferResult result = new TransferEngine().run(source, sink);

        assertThat(result.rowsWritten()).isEqualTo(2);

        try (Connection conn = DriverManager.getConnection(targetUrl)) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT name, age FROM customers WHERE id = 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Fay");
                assertThat(rs.getLong("age")).isEqualTo(28);
            }
        }
    }

    private void writeFixtureWorkbook(Path path) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Customers");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("name");
            header.createCell(2).setCellValue("age");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(1);
            row1.createCell(1).setCellValue("Fay");
            row1.createCell(2).setCellValue(28);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2);
            row2.createCell(1).setCellValue("Gus");
            row2.createCell(2).setCellValue(45);

            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                workbook.write(out);
            }
        }
    }
}
