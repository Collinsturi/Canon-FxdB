package com.fxdb.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryLogTest {

    @AfterEach
    void clearListener() {
        QueryLog.setListener(null);
    }

    @Test
    void recordCallsTheRegisteredListenerWithTheGivenValues() {
        List<QueryLog.Entry> received = new ArrayList<>();
        QueryLog.setListener(received::add);

        QueryLog.record(QueryLog.Role.SOURCE, "jdbc:postgresql://localhost/db", "SELECT * FROM customers");

        assertThat(received).hasSize(1);
        assertThat(received.get(0).role()).isEqualTo(QueryLog.Role.SOURCE);
        assertThat(received.get(0).jdbcUrl()).isEqualTo("jdbc:postgresql://localhost/db");
        assertThat(received.get(0).sql()).isEqualTo("SELECT * FROM customers");
        assertThat(received.get(0).timestamp()).isNotNull();
    }

    @Test
    void recordingWithNoListenerRegisteredDoesNothing() {
        QueryLog.record(QueryLog.Role.TARGET, "jdbc:sqlite:test.db", "SELECT 1");
        // No exception, no listener - nothing to assert beyond "didn't blow up."
    }
}
