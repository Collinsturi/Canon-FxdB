package com.fxdb.ui;

import com.fxdb.mapping.model.TableMapping;
import com.fxdb.transfer.ErrorPolicy;
import com.fxdb.transfer.MigrationRunResult;
import com.fxdb.ui.support.ConnectionSelection;

import java.util.ArrayList;
import java.util.List;

/** Carries the in-progress migration's state between wizard screens; one instance per Navigator, reset between runs. */
public class MigrationWizardContext {

    private ConnectionSelection source;
    private ConnectionSelection target;
    private List<TableMapping> tableMappings = new ArrayList<>();
    private ErrorPolicy errorPolicy = ErrorPolicy.SKIP_ROW;
    private int batchSize = 500;
    private MigrationRunResult lastRunResult;

    public ConnectionSelection getSource() {
        return source;
    }

    public void setSource(ConnectionSelection source) {
        this.source = source;
    }

    public ConnectionSelection getTarget() {
        return target;
    }

    public void setTarget(ConnectionSelection target) {
        this.target = target;
    }

    public List<TableMapping> getTableMappings() {
        return tableMappings;
    }

    public void setTableMappings(List<TableMapping> tableMappings) {
        this.tableMappings = tableMappings;
    }

    public ErrorPolicy getErrorPolicy() {
        return errorPolicy;
    }

    public void setErrorPolicy(ErrorPolicy errorPolicy) {
        this.errorPolicy = errorPolicy;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public MigrationRunResult getLastRunResult() {
        return lastRunResult;
    }

    public void setLastRunResult(MigrationRunResult lastRunResult) {
        this.lastRunResult = lastRunResult;
    }

    public void reset() {
        source = null;
        target = null;
        tableMappings = new ArrayList<>();
        lastRunResult = null;
    }
}
