package com.fxdb.ui.support;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.core.SchemaProvider;
import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.db.jdbc.JdbcDataSource;
import com.fxdb.db.jdbc.JdbcSchemaProvider;
import com.fxdb.file.FileConnectionConfig;
import com.fxdb.file.FileFormat;
import com.fxdb.file.FileSchemaProvider;
import com.fxdb.file.csv.CsvDataSink;
import com.fxdb.file.csv.CsvDataSource;
import com.fxdb.file.excel.ExcelDataSink;
import com.fxdb.file.excel.ExcelDataSource;
import com.fxdb.file.json.JsonDataSink;
import com.fxdb.file.json.JsonDataSource;
import com.fxdb.mapping.model.ConnectionDescriptor;

/**
 * Bridges a connection chosen in the UI (JDBC or one of the three file formats) into the
 * DataSource/DataSink/SchemaProvider instances the engine actually runs against. Kept free
 * of any JavaFX import so it's plain-JUnit testable on its own.
 */
public sealed interface ConnectionSelection {

    SchemaProvider createSchemaProvider();

    DataSource createDataSource(String tableName);

    DataSink createDataSink(String tableName);

    ConnectionDescriptor toDescriptor();

    record Jdbc(JdbcConnectionConfig config) implements ConnectionSelection {
        @Override
        public SchemaProvider createSchemaProvider() {
            return new JdbcSchemaProvider(config);
        }

        @Override
        public DataSource createDataSource(String tableName) {
            return new JdbcDataSource(config, tableName);
        }

        @Override
        public DataSink createDataSink(String tableName) {
            return new JdbcDataSink(config, tableName);
        }

        @Override
        public ConnectionDescriptor toDescriptor() {
            return ConnectionDescriptor.jdbc(config.jdbcUrl(), config.username());
        }
    }

    record File(FileConnectionConfig config) implements ConnectionSelection {
        @Override
        public SchemaProvider createSchemaProvider() {
            return new FileSchemaProvider(config);
        }

        @Override
        public DataSource createDataSource(String tableName) {
            FileConnectionConfig perTable = withSelectedTable(tableName);
            return switch (perTable.format()) {
                case CSV -> new CsvDataSource(perTable);
                case JSON -> new JsonDataSource(perTable);
                case EXCEL -> new ExcelDataSource(perTable);
            };
        }

        @Override
        public DataSink createDataSink(String tableName) {
            FileConnectionConfig perTable = withSelectedTable(tableName);
            return switch (perTable.format()) {
                case CSV -> new CsvDataSink(perTable);
                case JSON -> new JsonDataSink(perTable);
                case EXCEL -> new ExcelDataSink(perTable);
            };
        }

        /**
         * For Excel, "table" means sheet, so the config's sheetName is pinned to the one
         * requested. CSV/JSON address a single pseudo-table (the file itself); migrating more
         * than one source table into a CSV/JSON target isn't supported in this version - each
         * such table would overwrite the same file - so the config is used as-is.
         */
        private FileConnectionConfig withSelectedTable(String tableName) {
            if (config.format() != FileFormat.EXCEL) {
                return config;
            }
            return new FileConnectionConfig(config.path(), config.format(), config.delimiter(),
                    config.hasHeader(), tableName);
        }

        @Override
        public ConnectionDescriptor toDescriptor() {
            return ConnectionDescriptor.file(config.path().toString(), config.format().name());
        }
    }
}
