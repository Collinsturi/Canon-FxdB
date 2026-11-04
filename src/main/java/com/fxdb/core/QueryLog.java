package com.fxdb.core;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A single global pub point for the real SQL/DDL this app runs against a database - plain Java,
 * no JavaFX dependency, same as {@code ProgressListener}, so db.jdbc/transfer stay usable from
 * plain unit tests. The UI is the one that marshals onto the FX thread when it registers its
 * listener; {@link #record} just calls whatever's registered, synchronously, on the caller's
 * own thread.
 */
public final class QueryLog {

    /** Which side of a migration a statement ran against - lets the UI tag/color entries. */
    public enum Role {
        SOURCE, TARGET
    }

    public record Entry(Instant timestamp, Role role, String jdbcUrl, String sql) {
    }

    private static volatile Consumer<Entry> listener = entry -> {
    };

    private QueryLog() {
    }

    public static void setListener(Consumer<Entry> newListener) {
        listener = newListener == null ? entry -> {
        } : newListener;
    }

    public static void record(Role role, String jdbcUrl, String sql) {
        listener.accept(new Entry(Instant.now(), role, jdbcUrl, sql));
    }
}
