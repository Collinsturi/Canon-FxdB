package com.fxdb.mapping.model;

import java.util.ArrayList;
import java.util.List;

/** The root of a saved/reloadable migration configuration: where from, where to, and how each table maps. */
public class MigrationProfile {

    private ConnectionDescriptor source;
    private ConnectionDescriptor target;
    private List<TableMapping> tableMappings = new ArrayList<>();

    public MigrationProfile() {
    }

    public MigrationProfile(ConnectionDescriptor source, ConnectionDescriptor target, List<TableMapping> tableMappings) {
        this.source = source;
        this.target = target;
        this.tableMappings = tableMappings;
    }

    public ConnectionDescriptor getSource() {
        return source;
    }

    public void setSource(ConnectionDescriptor source) {
        this.source = source;
    }

    public ConnectionDescriptor getTarget() {
        return target;
    }

    public void setTarget(ConnectionDescriptor target) {
        this.target = target;
    }

    public List<TableMapping> getTableMappings() {
        return tableMappings;
    }

    public void setTableMappings(List<TableMapping> tableMappings) {
        this.tableMappings = tableMappings;
    }
}
