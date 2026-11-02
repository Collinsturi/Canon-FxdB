package com.fxdb.file;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSource;
import com.fxdb.file.csv.CsvDataSink;
import com.fxdb.transfer.TransferEngine;
import com.fxdb.transfer.TransferResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteToCsvSmokeTest {

    @Test
    void migratesSqliteTableIntoCsv(@TempDir Path tempDir) throws SQLException, IOException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        seedSourceDatabase(sourceUrl);

        Path csvOut = tempDir.resolve("customers-export.csv");
        JdbcDataSource source = new JdbcDataSource(new JdbcConnectionConfig(sourceUrl, null, null), "customers");
        CsvDataSink sink = new CsvDataSink(FileConnectionConfig.csv(csvOut));

        TransferResult result = new TransferEngine().run(source, sink);

        assertThat(result.rowsWritten()).isEqualTo(2);

        List<String> lines = Files.readAllLines(csvOut);
        assertThat(lines.get(0)).isEqualTo("id,name,age");
        assertThat(lines).anyMatch(line -> line.equals("1,Dana,22"));
        assertThat(lines).anyMatch(line -> line.equals("2,Evan,33"));
    }

    private void seedSourceDatabase(String jdbcUrl) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL, age INTEGER)");
            statement.execute("INSERT INTO customers (id, name, age) VALUES (1, 'Dana', 22)");
            statement.execute("INSERT INTO customers (id, name, age) VALUES (2, 'Evan', 33)");
        }
    }
}
