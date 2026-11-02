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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * batchSize=1 makes each batch exactly one row, which turns the engine's batch-level error
 * handling into effectively per-row handling for this test - the simplest way to prove
 * ABORT/SKIP_ROW/RETRY_THEN_SKIP all behave as intended without a flakier test double.
 */
class TransferEngineErrorPolicyTest {

    @Test
    void abortStopsAtTheFirstFailureAndMarksTheTableFailed(@TempDir Path tempDir) throws SQLException {
        Fixture fixture = seedFixture(tempDir);
        RecordingListener listener = new RecordingListener();

        TableTransferResult result = new TransferEngine().runWithProgress(
                fixture.source(), fixture.sink(), fixture.mapping(),
                ErrorPolicy.ABORT, listener, new AtomicBoolean(false), 1);

        assertThat(result.status()).isEqualTo(TableTransferResult.Status.FAILED);
        assertThat(result.rowsWritten()).isEqualTo(1); // only id=1 made it in before id=2 failed
        assertThat(result.errors()).hasSize(1);
        assertThat(listener.errors).hasSize(1);
    }

    @Test
    void skipRowContinuesPastTheFailureAndMarksTheTablePartial(@TempDir Path tempDir) throws SQLException {
        Fixture fixture = seedFixture(tempDir);

        TableTransferResult result = new TransferEngine().runWithProgress(
                fixture.source(), fixture.sink(), fixture.mapping(),
                ErrorPolicy.SKIP_ROW, ProgressListener.NO_OP, new AtomicBoolean(false), 1);

        assertThat(result.status()).isEqualTo(TableTransferResult.Status.PARTIAL);
        assertThat(result.rowsWritten()).isEqualTo(2); // id=1 and id=3
        assertThat(result.rowsSkipped()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void retryThenSkipFallsBackToSkippingAPersistentFailure(@TempDir Path tempDir) throws SQLException {
        Fixture fixture = seedFixture(tempDir);

        TableTransferResult result = new TransferEngine().runWithProgress(
                fixture.source(), fixture.sink(), fixture.mapping(),
                ErrorPolicy.RETRY_THEN_SKIP, ProgressListener.NO_OP, new AtomicBoolean(false), 1);

        assertThat(result.status()).isEqualTo(TableTransferResult.Status.PARTIAL);
        assertThat(result.rowsWritten()).isEqualTo(2);
        assertThat(result.rowsSkipped()).isEqualTo(1);
    }

    private record Fixture(JdbcDataSource source, JdbcDataSink sink, TableMapping mapping) {
    }

    private Fixture seedFixture(Path tempDir) throws SQLException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        try (Connection conn = DriverManager.getConnection(sourceUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT)");
            statement.execute("INSERT INTO t (id, name) VALUES (1, 'Alice')");
            statement.execute("INSERT INTO t (id, name) VALUES (2, NULL)");
            statement.execute("INSERT INTO t (id, name) VALUES (3, 'Carol')");
        }
        // Pre-existing target table (not auto-created) with a NOT NULL constraint the source
        // row with a null name will violate.
        try (Connection conn = DriverManager.getConnection(targetUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE out_t (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
        }

        JdbcConnectionConfig sourceConfig = new JdbcConnectionConfig(sourceUrl, null, null);
        JdbcConnectionConfig targetConfig = new JdbcConnectionConfig(targetUrl, null, null);

        var sourceSchema = withSchemaProvider(sourceConfig, p -> p.describeTable("t"));
        var targetSchema = withSchemaProvider(targetConfig, p -> p.describeTable("out_t"));
        TableMapping mapping = MappingEngine.autoMatch(sourceSchema, "out_t", targetSchema);

        return new Fixture(
                new JdbcDataSource(sourceConfig, "t"),
                new JdbcDataSink(targetConfig, "out_t"),
                mapping);
    }

    private <T> T withSchemaProvider(JdbcConnectionConfig config, java.util.function.Function<JdbcSchemaProvider, T> fn) {
        try (JdbcSchemaProvider provider = new JdbcSchemaProvider(config)) {
            return fn.apply(provider);
        }
    }

    private static final class RecordingListener implements ProgressListener {
        final List<String> errors = new ArrayList<>();

        @Override
        public void onTableStart(String tableName, long estimatedRows) {
        }

        @Override
        public void onBatchProgress(String tableName, long rowsDoneSoFar) {
        }

        @Override
        public void onTableComplete(TableTransferResult result) {
        }

        @Override
        public void onError(String tableName, String errorMessage) {
            errors.add(errorMessage);
        }
    }
}
