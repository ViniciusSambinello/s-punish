package com.spunish.common.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Refuses to start if the database schema is already on a newer version than
 * this binary understands.
 */
public final class SchemaMigrator {

    private static final int LOCK_TIMEOUT_SECONDS = 30;

    private final DataSource dataSource;
    private final TableNames tables;
    private final Logger logger;
    private final String lockName;

    public SchemaMigrator(DataSource dataSource, TableNames tables, Logger logger) {
        this.dataSource = dataSource;
        this.tables = tables;
        this.logger = logger;
        this.lockName = "spunish_migration_" + tables.schemaVersion();
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            acquireLock(connection);
            try {
                int current = currentVersion(connection);
                if (current > Migrations.CURRENT_VERSION) {
                    throw new SchemaVersionTooNewException(current, Migrations.CURRENT_VERSION);
                }
                for (int version = current + 1; version <= Migrations.CURRENT_VERSION; version++) {
                    applyVersion(connection, version);
                }
            } finally {
                releaseLock(connection);
            }
        }
    }

    private void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                Object acquired = result.getObject(1);
                if (!(acquired instanceof Number number) || number.intValue() != 1) {
                    throw new SQLException("Could not acquire schema migration lock '" + lockName
                            + "' within " + LOCK_TIMEOUT_SECONDS + "s");
                }
            }
        }
    }

    private void releaseLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            statement.execute();
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        if (!tableExists(connection, tables.schemaVersion())) {
            return 0;
        }
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT MAX(version) FROM `" + tables.schemaVersion() + "`")) {
            if (result.next()) {
                int value = result.getInt(1);
                return result.wasNull() ? 0 : value;
            }
            return 0;
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void applyVersion(Connection connection, int version) throws SQLException {
        logger.info("Applying schema migration V" + version + "...");
        for (String statementSql : Migrations.statementsFor(version, tables)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tables.schemaVersion() + "` (version, applied_at) VALUES (?, UTC_TIMESTAMP(3))")) {
            insert.setInt(1, version);
            insert.execute();
        }
        logger.info("Schema migration V" + version + " applied.");
    }
}
