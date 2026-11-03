package com.fxdb.db.jdbc;

import com.fxdb.db.jdbc.dialect.MySqlDialect;
import com.fxdb.db.jdbc.dialect.OracleDialect;
import com.fxdb.db.jdbc.dialect.PostgresDialect;
import com.fxdb.db.jdbc.dialect.SqlDialect;
import com.fxdb.db.jdbc.dialect.SqlServerDialect;
import com.fxdb.db.jdbc.dialect.SqliteDialect;

public enum DriverRegistry {
    POSTGRES("org.postgresql.Driver", new PostgresDialect(), "PostgreSQL", 5432),
    MYSQL("com.mysql.cj.jdbc.Driver", new MySqlDialect(), "MySQL", 3306),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", new SqlServerDialect(), "SQL Server", 1433),
    ORACLE("oracle.jdbc.OracleDriver", new OracleDialect(), "Oracle", 1521),
    SQLITE("org.sqlite.JDBC", new SqliteDialect(), "SQLite", -1);

    private final String driverClassName;
    private final SqlDialect dialect;
    private final String displayName;
    private final int defaultPort;

    DriverRegistry(String driverClassName, SqlDialect dialect, String displayName, int defaultPort) {
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        this.displayName = displayName;
        this.defaultPort = defaultPort;
    }

    public String driverClassName() {
        return driverClassName;
    }

    public SqlDialect dialect() {
        return dialect;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isFileBased() {
        return this == SQLITE;
    }

    /**
     * Unused for SQLITE, which has no host/port to default.
     */
    public int defaultPort() {
        return defaultPort;
    }

    /**
     * Builds the JDBC URL for a server-based provider from plain host/port/database
     * fields. Not applicable to SQLITE - use {@link #buildSqliteUrl} instead.
     */
    public String buildUrl(String host, int port, String database) {
        return switch (this) {
            case POSTGRES -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database;
            case SQLSERVER -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database;
            // The "@//host:port/service" form works for both SID and service name,
            // unlike the older positional "@host:port:SID" form.
            case ORACLE -> "jdbc:oracle:thin:@//" + host + ":" + port + "/" + database;
            case SQLITE -> throw new IllegalStateException("SQLite has no host/port URL - use buildSqliteUrl instead");
        };
    }

    public static String buildSqliteUrl(String path) {
        return "jdbc:sqlite:" + path;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static DriverRegistry fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:postgresql:")) return POSTGRES;
        if (jdbcUrl.startsWith("jdbc:mysql:")) return MYSQL;
        if (jdbcUrl.startsWith("jdbc:sqlserver:")) return SQLSERVER;
        if (jdbcUrl.startsWith("jdbc:oracle:")) return ORACLE;
        if (jdbcUrl.startsWith("jdbc:sqlite:")) return SQLITE;
        throw new IllegalArgumentException("Unrecognized JDBC URL scheme: " + jdbcUrl);
    }
}
