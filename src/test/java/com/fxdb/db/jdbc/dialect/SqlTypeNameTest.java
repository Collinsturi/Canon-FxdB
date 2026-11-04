package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.DataType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlTypeNameTest {

    @Test
    void sqlServerHasNoBooleanDoublePrecisionOrBlob() {
        SqlServerDialect dialect = new SqlServerDialect();
        assertThat(dialect.sqlTypeName(DataType.BOOLEAN)).isEqualTo("BIT");
        assertThat(dialect.sqlTypeName(DataType.DOUBLE)).isEqualTo("FLOAT");
        assertThat(dialect.sqlTypeName(DataType.BYTES)).isEqualTo("VARBINARY(MAX)");
        assertThat(dialect.sqlTypeName(DataType.TIMESTAMP)).isEqualTo("DATETIME2");
        // Unaffected types still fall through to the ANSI default.
        assertThat(dialect.sqlTypeName(DataType.INTEGER)).isEqualTo("INTEGER");
        assertThat(dialect.sqlTypeName(DataType.DATE)).isEqualTo("DATE");
    }

    @Test
    void postgresHasNoBlob() {
        PostgresDialect dialect = new PostgresDialect();
        assertThat(dialect.sqlTypeName(DataType.BYTES)).isEqualTo("BYTEA");
        assertThat(dialect.sqlTypeName(DataType.BOOLEAN)).isEqualTo("BOOLEAN");
        assertThat(dialect.sqlTypeName(DataType.DOUBLE)).isEqualTo("DOUBLE PRECISION");
    }

    @Test
    void oracleHasNoNativeBoolean() {
        OracleDialect dialect = new OracleDialect();
        assertThat(dialect.sqlTypeName(DataType.BOOLEAN)).isEqualTo("NUMBER(1)");
        assertThat(dialect.sqlTypeName(DataType.BYTES)).isEqualTo("BLOB");
    }

    @Test
    void mySqlAndSqliteUseTheAnsiDefaultsAsIs() {
        assertThat(new MySqlDialect().sqlTypeName(DataType.BOOLEAN)).isEqualTo("BOOLEAN");
        assertThat(new SqliteDialect().sqlTypeName(DataType.BOOLEAN)).isEqualTo("BOOLEAN");
    }
}
