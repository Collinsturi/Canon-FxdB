package com.fxdb.db.jdbc;

import java.sql.SQLException;

/**
 * Builds a human-readable description of a {@link SQLException} that keeps the driver's own
 * message (the actual text the database server returned, e.g. an auth failure or a timeout)
 * instead of letting callers discard it behind a generic wrapper message.
 */
public final class JdbcErrors {

    private JdbcErrors() {
    }

    public static String describe(SQLException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        String sqlState = e.getSQLState();
        String described = sqlState == null || sqlState.isBlank()
                ? message
                : message + " [SQLState: " + sqlState + "]";

        // A failed executeBatch() (java.sql.BatchUpdateException included) commonly carries the
        // real per-statement error as a chained exception rather than in the top-level message.
        SQLException next = e.getNextException();
        if (next != null && next != e) {
            described = described + "; " + describe(next);
        }
        return described;
    }
}
