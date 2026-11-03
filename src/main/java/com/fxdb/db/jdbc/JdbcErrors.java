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
        return sqlState == null || sqlState.isBlank()
                ? message
                : message + " [SQLState: " + sqlState + "]";
    }
}
