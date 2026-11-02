package com.fxdb.file;

import java.nio.file.Path;

/**
 * Connection config for a flat-file source/sink, mirroring JdbcConnectionConfig's role for
 * the file side of the database-agnostic transfer pipeline.
 *
 * @param sheetName only meaningful for FileFormat.EXCEL; null selects the first sheet on read,
 *                  or defaults to "Sheet1" on write.
 */
public record FileConnectionConfig(
        Path path,
        FileFormat format,
        char delimiter,
        boolean hasHeader,
        String sheetName
) {
    public static FileConnectionConfig csv(Path path) {
        return new FileConnectionConfig(path, FileFormat.CSV, ',', true, null);
    }

    public static FileConnectionConfig csv(Path path, char delimiter) {
        return new FileConnectionConfig(path, FileFormat.CSV, delimiter, true, null);
    }

    public static FileConnectionConfig json(Path path) {
        return new FileConnectionConfig(path, FileFormat.JSON, ',', true, null);
    }

    public static FileConnectionConfig excel(Path path, String sheetName) {
        return new FileConnectionConfig(path, FileFormat.EXCEL, ',', true, sheetName);
    }

    /** The pseudo-table name a CSV/JSON file is addressed by: its filename without extension. */
    public String fileBaseName() {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
