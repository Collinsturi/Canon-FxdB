package com.fxdb.transfer;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.db.jdbc.JdbcDataSource;
import com.fxdb.db.jdbc.JdbcSchemaProvider;
import com.fxdb.mapping.MappingEngine;
import com.fxdb.mapping.model.TableMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves MappingEngine's auto-matched TableMapping actually drives a real transfer, renaming
 * columns along the way - the point where Phase 3's mapping engine and Phase 1's transfer
 * engine are wired together for real.
 */
class TransferEngineWithMappingTest {

    @Test
    void autoMatchedMappingRenamesColumnsDuringTransfer(@TempDir Path tempDir) throws SQLException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        try (Connection conn = DriverManager.getConnection(sourceUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE customers (customer_id INTEGER PRIMARY KEY, full_name TEXT NOT NULL)");
            statement.execute("INSERT INTO customers (customer_id, full_name) VALUES (1, 'Hana')");
            statement.execute("INSERT INTO customers (customer_id, full_name) VALUES (2, 'Ivo')");
        }
        // The existing target table uses different column names than the source.
        try (Connection conn = DriverManager.getConnection(targetUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE clients (cust_id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
        }

        JdbcConnectionConfig sourceConfig = new JdbcConnectionConfig(sourceUrl, null, null);
        JdbcConnectionConfig targetConfig = new JdbcConnectionConfig(targetUrl, null, null);

        var sourceSchema = withSchemaProvider(sourceConfig, p -> p.describeTable("customers"));
        var targetSchema = withSchemaProvider(targetConfig, p -> p.describeTable("clients"));

        TableMapping mapping = MappingEngine.autoMatch(sourceSchema, "clients", targetSchema);

        JdbcDataSource source = new JdbcDataSource(sourceConfig, "customers");
        JdbcDataSink sink = new JdbcDataSink(targetConfig, "clients");

        TransferResult result = new TransferEngine().run(source, sink, mapping);

        assertThat(result.rowsWritten()).isEqualTo(2);

        try (Connection conn = DriverManager.getConnection(targetUrl);
             ResultSet rs = conn.createStatement().executeQuery("SELECT name FROM clients WHERE cust_id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("Hana");
        }
    }

    private <T> T withSchemaProvider(JdbcConnectionConfig config, java.util.function.Function<JdbcSchemaProvider, T> fn) {
        try (JdbcSchemaProvider provider = new JdbcSchemaProvider(config)) {
            return fn.apply(provider);
        }
    }
}
