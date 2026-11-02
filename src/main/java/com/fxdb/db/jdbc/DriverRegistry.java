package com.fxdb.db.jdbc;

import com.fxdb.db.jdbc.dialect.MySqlDialect;
import com.fxdb.db.jdbc.dialect.OracleDialect;
import com.fxdb.db.jdbc.dialect.PostgresDialect;
import com.fxdb.db.jdbc.dialect.SqlDialect;
import com.fxdb.db.jdbc.dialect.SqlServerDialect;
import com.fxdb.db.jdbc.dialect.SqliteDialect;

public enum DriverRegistry {
    POSTGRES("org.postgresql.Driver", new PostgresDialect()),
    MYSQL("com.mysql.cj.jdbc.Driver", new MySqlDialect()),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", new SqlServerDialect()),
    ORACLE("oracle.jdbc.OracleDriver", new OracleDialect()),
    SQLITE("org.sqlite.JDBC", new SqliteDialect());

    private final String driverClassName;
    private final SqlDialect dialect;

    DriverRegistry(String driverClassName, SqlDialect dialect) {
        this.driverClassName = driverClassName;
        this.dialect = dialect;
    }

    public String driverClassName() {
        return driverClassName;
    }

    public SqlDialect dialect() {
        return dialect;
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
