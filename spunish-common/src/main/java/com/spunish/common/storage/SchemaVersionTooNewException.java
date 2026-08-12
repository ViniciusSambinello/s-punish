package com.spunish.common.storage;

/**
 * Thrown when {@code schema_version} in the database is newer than this
 * plugin binary supports — running anyway risks corrupting data written by a
 * newer schema this version does not understand.
 */
public final class SchemaVersionTooNewException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SchemaVersionTooNewException(int databaseVersion, int supportedVersion) {
        super("Database schema version " + databaseVersion + " is newer than the "
                + supportedVersion + " this plugin version supports. Refusing to start — update the plugin.");
    }
}
