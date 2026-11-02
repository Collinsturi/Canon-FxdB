package com.fxdb.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fxdb.core.exception.FxDbException;
import com.fxdb.mapping.model.MigrationProfile;

import java.io.IOException;
import java.nio.file.Path;

/** Saves/loads a MigrationProfile as JSON so a mapping can be reused without redoing it by hand. */
public final class MappingProfileSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private MappingProfileSerializer() {
    }

    public static void save(MigrationProfile profile, Path path) {
        try {
            MAPPER.writeValue(path.toFile(), profile);
        } catch (IOException e) {
            throw new FxDbException("Failed to save migration profile to " + path, e);
        }
    }

    public static MigrationProfile load(Path path) {
        try {
            return MAPPER.readValue(path.toFile(), MigrationProfile.class);
        } catch (IOException e) {
            throw new FxDbException("Failed to load migration profile from " + path, e);
        }
    }
}
