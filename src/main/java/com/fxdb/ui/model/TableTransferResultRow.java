package com.fxdb.ui.model;

import com.fxdb.transfer.TableTransferResult;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/** Read-only TableView row summarizing one table's TableTransferResult on the execution/summary screens. */
public class TableTransferResultRow {

    private final SimpleStringProperty tableName;
    private final SimpleLongProperty rowsRead;
    private final SimpleLongProperty rowsWritten;
    private final SimpleLongProperty rowsSkipped;
    private final SimpleStringProperty status;
    private final SimpleLongProperty errorCount;

    public TableTransferResultRow(TableTransferResult result) {
        this.tableName = new SimpleStringProperty(result.tableName());
        this.rowsRead = new SimpleLongProperty(result.rowsRead());
        this.rowsWritten = new SimpleLongProperty(result.rowsWritten());
        this.rowsSkipped = new SimpleLongProperty(result.rowsSkipped());
        this.status = new SimpleStringProperty(result.status().name());
        this.errorCount = new SimpleLongProperty(result.errors().size());
    }

    public String getTableName() {
        return tableName.get();
    }

    public long getRowsRead() {
        return rowsRead.get();
    }

    public long getRowsWritten() {
        return rowsWritten.get();
    }

    public long getRowsSkipped() {
        return rowsSkipped.get();
    }

    public String getStatus() {
        return status.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }
}
