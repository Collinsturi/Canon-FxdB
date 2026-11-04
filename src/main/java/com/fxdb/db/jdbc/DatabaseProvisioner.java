package com.fxdb.db.jdbc;

import com.fxdb.core.exception.FxDbException;
import com.fxdb.db.jdbc.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Creates a target database that doesn't exist yet - so a connection with no database selected
 * yet can still be turned into a usable migration target, including the "duplicate this
 * database" case where source and target point at the same server under different names.
 */
public final class DatabaseProvisioner {

    private DatabaseProvisioner() {
    }

    /**
     * @param adminUrl a JDBC URL already pointed at {@link SqlDialect#adminDatabase()} - built
     *                 by the caller the same way it builds the real target URL, so any extra
     *                 connection properties (SQL Server's encrypt/trustServerCertificate/
     *                 authentication, say) carry over to this connection too. A URL built fresh
     *                 from just host/port here would silently drop those and could fail (e.g. a
     *                 TLS trust error) even though the real target connection works fine.
     * @return true if the database was just created, false if it already existed.
     */
    public static boolean ensureDatabaseExists(DriverRegistry provider, String adminUrl, String databaseName,
                                                String username, String password) {
        SqlDialect dialect = provider.dialect();
        if (!dialect.supportsDatabaseCreation()) {
            throw new FxDbException("Creating a database isn't supported for " + provider.displayName());
        }

        try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
            if (databaseExists(connection, databaseName)) {
                return false;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(dialect.createDatabaseDdl(databaseName));
            }
            return true;
        } catch (SQLException e) {
            throw new FxDbException(
                    "Failed to create database '" + databaseName + "': " + JdbcErrors.describe(e), e);
        }
    }

    private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        try (ResultSet catalogs = connection.getMetaData().getCatalogs()) {
            while (catalogs.next()) {
                if (catalogs.getString("TABLE_CAT").toLowerCase(Locale.ROOT)
                        .equals(databaseName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
