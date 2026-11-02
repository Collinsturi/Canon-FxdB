package com.fxdb.mapping.model;

import com.fxdb.core.model.DataType;

/**
 * A mutable POJO rather than a record on purpose: Phase 4's mapping grid will wrap this in a
 * JavaFX-bean adapter bound directly to a TableView, and edits there need to mutate the
 * underlying mapping in place.
 */
public class ColumnMapping {

    private String sourceColumn;
    private String targetColumn;
    private DataType targetType;
    private ColumnTransform transform = ColumnTransform.NONE;
    private double matchConfidence;
    private boolean included = true;

    public ColumnMapping() {
    }

    public ColumnMapping(String sourceColumn, String targetColumn, DataType targetType,
                          ColumnTransform transform, double matchConfidence, boolean included) {
        this.sourceColumn = sourceColumn;
        this.targetColumn = targetColumn;
        this.targetType = targetType;
        this.transform = transform;
        this.matchConfidence = matchConfidence;
        this.included = included;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

    public DataType getTargetType() {
        return targetType;
    }

    public void setTargetType(DataType targetType) {
        this.targetType = targetType;
    }

    public ColumnTransform getTransform() {
        return transform;
    }

    public void setTransform(ColumnTransform transform) {
        this.transform = transform;
    }

    public double getMatchConfidence() {
        return matchConfidence;
    }

    public void setMatchConfidence(double matchConfidence) {
        this.matchConfidence = matchConfidence;
    }

    public boolean isIncluded() {
        return included;
    }

    public void setIncluded(boolean included) {
        this.included = included;
    }
}
