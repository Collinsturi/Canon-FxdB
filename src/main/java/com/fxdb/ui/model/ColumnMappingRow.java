package com.fxdb.ui.model;

import com.fxdb.core.model.DataType;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.ColumnTransform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX-bean view of a ColumnMapping for TableView binding in the mapping grid. Property
 * changes write straight back into the underlying ColumnMapping, since that's the mutable
 * object MappingProfileSerializer will persist and TransferEngine will actually run with.
 */
public class ColumnMappingRow {

    private final ColumnMapping mapping;
    private final String sourceType;

    private final StringProperty sourceColumn;
    private final StringProperty targetColumn;
    private final ObjectProperty<DataType> targetType;
    private final ObjectProperty<ColumnTransform> transform;
    private final BooleanProperty included;
    private final javafx.beans.property.DoubleProperty matchConfidence;

    public ColumnMappingRow(ColumnMapping mapping, String sourceType) {
        this.mapping = mapping;
        this.sourceType = sourceType;

        this.sourceColumn = new SimpleStringProperty(mapping.getSourceColumn());
        this.targetColumn = new SimpleStringProperty(mapping.getTargetColumn());
        this.targetType = new SimpleObjectProperty<>(mapping.getTargetType());
        this.transform = new SimpleObjectProperty<>(mapping.getTransform());
        this.included = new SimpleBooleanProperty(mapping.isIncluded());
        this.matchConfidence = new javafx.beans.property.SimpleDoubleProperty(mapping.getMatchConfidence());

        targetColumn.addListener((obs, old, value) -> mapping.setTargetColumn(value));
        targetType.addListener((obs, old, value) -> mapping.setTargetType(value));
        transform.addListener((obs, old, value) -> mapping.setTransform(value));
        included.addListener((obs, old, value) -> mapping.setIncluded(value));
    }

    public ColumnMapping underlying() {
        return mapping;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceColumn() {
        return sourceColumn.get();
    }

    public StringProperty sourceColumnProperty() {
        return sourceColumn;
    }

    public String getTargetColumn() {
        return targetColumn.get();
    }

    public void setTargetColumn(String value) {
        targetColumn.set(value);
    }

    public StringProperty targetColumnProperty() {
        return targetColumn;
    }

    public DataType getTargetType() {
        return targetType.get();
    }

    public void setTargetType(DataType value) {
        targetType.set(value);
    }

    public ObjectProperty<DataType> targetTypeProperty() {
        return targetType;
    }

    public ColumnTransform getTransform() {
        return transform.get();
    }

    public void setTransform(ColumnTransform value) {
        transform.set(value);
    }

    public ObjectProperty<ColumnTransform> transformProperty() {
        return transform;
    }

    public boolean isIncluded() {
        return included.get();
    }

    public void setIncluded(boolean value) {
        included.set(value);
    }

    public BooleanProperty includedProperty() {
        return included;
    }

    public double getMatchConfidence() {
        return matchConfidence.get();
    }

    public javafx.beans.property.DoubleProperty matchConfidenceProperty() {
        return matchConfidence;
    }
}
