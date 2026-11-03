package com.fxdb.db.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcErrorsTest {

    @Test
    void includesMessageAndSqlState() {
        SQLException e = new SQLException("connection refused", "08001");
        assertThat(JdbcErrors.describe(e)).isEqualTo("connection refused [SQLState: 08001]");
    }

    @Test
    void fallsBackToClassNameWhenMessageIsBlank() {
        SQLException e = new SQLException((String) null);
        assertThat(JdbcErrors.describe(e)).isEqualTo("SQLException");
    }

    @Test
    void omitsSqlStateWhenAbsent() {
        SQLException e = new SQLException("boom");
        assertThat(JdbcErrors.describe(e)).isEqualTo("boom");
    }

    @Test
    void foldsInChainedExceptionFromFailedBatch() {
        SQLException first = new SQLException("Batch entry 0 was aborted", "08007");
        SQLException real = new SQLException("String or binary data would be truncated", "22001");
        first.setNextException(real);

        assertThat(JdbcErrors.describe(first)).isEqualTo(
                "Batch entry 0 was aborted [SQLState: 08007]; "
                        + "String or binary data would be truncated [SQLState: 22001]");
    }
}
