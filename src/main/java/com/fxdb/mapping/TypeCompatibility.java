package com.fxdb.mapping;

import com.fxdb.core.model.DataType;

/** A static compatibility score between two unified DataTypes, used as one input to auto-matching. */
public final class TypeCompatibility {

    private TypeCompatibility() {
    }

    public static double score(DataType a, DataType b) {
        if (a == b) {
            return 1.0;
        }
        if (a == DataType.STRING || b == DataType.STRING) {
            return 0.3;
        }
        if (isNumeric(a) && isNumeric(b)) {
            return numericPairScore(a, b);
        }
        if (isTemporal(a) && isTemporal(b)) {
            return 0.8;
        }
        return 0.0;
    }

    private static double numericPairScore(DataType a, DataType b) {
        if (isWidePair(a, b, DataType.INTEGER, DataType.LONG)) {
            return 0.9;
        }
        if (isWidePair(a, b, DataType.DOUBLE, DataType.DECIMAL)) {
            return 0.8;
        }
        return 0.6;
    }

    private static boolean isWidePair(DataType a, DataType b, DataType x, DataType y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    private static boolean isNumeric(DataType type) {
        return type == DataType.INTEGER || type == DataType.LONG
                || type == DataType.DOUBLE || type == DataType.DECIMAL;
    }

    private static boolean isTemporal(DataType type) {
        return type == DataType.DATE || type == DataType.TIMESTAMP;
    }
}
