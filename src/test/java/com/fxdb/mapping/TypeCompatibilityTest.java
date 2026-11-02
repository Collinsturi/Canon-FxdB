package com.fxdb.mapping;

import com.fxdb.core.model.DataType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TypeCompatibilityTest {

    @ParameterizedTest
    @CsvSource({
            "LONG, LONG, 1.0",
            "STRING, STRING, 1.0",
            "INTEGER, LONG, 0.9",
            "LONG, INTEGER, 0.9",
            "DOUBLE, DECIMAL, 0.8",
            "DATE, TIMESTAMP, 0.8",
            "STRING, LONG, 0.3",
            "LONG, STRING, 0.3",
            "BOOLEAN, LONG, 0.0",
            "BYTES, BOOLEAN, 0.0"
    })
    void scoresKnownTypePairs(DataType a, DataType b, double expected) {
        assertThat(TypeCompatibility.score(a, b)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "INTEGER, DOUBLE",
            "LONG, DOUBLE",
            "INTEGER, DECIMAL"
    })
    void looselyRelatedNumericPairsScoreBetweenZeroAndExactMatch(DataType a, DataType b) {
        double score = TypeCompatibility.score(a, b);
        assertThat(score).isGreaterThan(0.0).isLessThan(1.0);
    }
}
