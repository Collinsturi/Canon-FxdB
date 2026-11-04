package com.fxdb.db.jdbc;

import com.fxdb.core.QueryLog;
import com.fxdb.core.exception.FxDbException;
import com.fxdb.core.model.TableSchema;
import com.fxdb.db.jdbc.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Replicates every table's structure from a source database into a target one - used when
 * creating a brand-new target database, so it isn't left empty, and for the "duplicate this
 * database" case where source and target are the same server under different names. Tables the
 * target already has are left alone, so this is safe to run more than once.
 */
public final class SchemaCopier {

    private SchemaCopier() {
    }

    /** @return how many tables were actually created (existing ones are skipped, not recreated). */
    public static int copyAllTables(JdbcConnectionConfig sourceConfig, JdbcConnectionConfig targetConfig) {
        SqlDialect targetDialect = DriverRegistry.fromJdbcUrl(targetConfig.jdbcUrl()).dialect();
        int copied = 0;

        try (JdbcSchemaProvider sourceProvider = new JdbcSchemaProvider(sourceConfig);
             Connection targetConnection = DriverManager.getConnection(
                     targetConfig.jdbcUrl(), targetConfig.username(), targetConfig.password())) {

            Set<String> existingTargetTables = existingTableNamesLowerCase(targetConnection);

            for (String tableName : sourceProvider.listTables()) {
                if (existingTargetTables.contains(tableName.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                TableSchema schema = sourceProvider.describeTable(tableName);
                String ddl = targetDialect.createTableDdl(schema);
                QueryLog.record(QueryLog.Role.TARGET, targetConfig.jdbcUrl(), ddl);
                try (Statement statement = targetConnection.createStatement()) {
                    statement.execute(ddl);
                }
                copied++;
            }
        } catch (SQLException e) {
            throw new FxDbException(
                    "Failed to copy schema to " + targetConfig.jdbcUrl() + ": " + JdbcErrors.describe(e), e);
        }
        return copied;
    }

    private static Set<String> existingTableNamesLowerCase(Connection connection) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet rs = connection.getMetaData().getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }
}
