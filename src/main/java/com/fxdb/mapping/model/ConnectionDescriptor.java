package com.fxdb.mapping.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A serializable description of a source or target connection, deliberately kept as a loose
 * kind + key/value settings bag rather than a typed class per database/file type - so adding
 * a new connector later doesn't require touching the profile format. Passwords are never
 * stored here: callers that need "remember password" will have to layer that on themselves,
 * since this is a plain JSON profile, not a secure credential store.
 */
public class ConnectionDescriptor {

    private String kind; // e.g. "JDBC", "FILE"
    private Map<String, String> settings = new LinkedHashMap<>();

    public ConnectionDescriptor() {
    }

    public ConnectionDescriptor(String kind, Map<String, String> settings) {
        this.kind = kind;
        this.settings = settings;
    }

    public static ConnectionDescriptor jdbc(String jdbcUrl, String username) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("jdbcUrl", jdbcUrl);
        if (username != null) {
            settings.put("username", username);
        }
        return new ConnectionDescriptor("JDBC", settings);
    }

    public static ConnectionDescriptor file(String path, String format) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("path", path);
        settings.put("format", format);
        return new ConnectionDescriptor("FILE", settings);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}
