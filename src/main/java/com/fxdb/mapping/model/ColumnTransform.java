package com.fxdb.mapping.model;

/**
 * A value transform applied while copying a source column into a target column.
 * Only NONE and TRIM are actually executed by RowConverter in this version; DATE_FORMAT,
 * CAST and CUSTOM_EXPRESSION are reserved slots for the mapping grid to offer once the UI
 * (Phase 4) needs them, and currently pass the value through unchanged.
 */
public enum ColumnTransform {
    NONE,
    TRIM,
    DATE_FORMAT,
    CAST,
    CUSTOM_EXPRESSION
}
