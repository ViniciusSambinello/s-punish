package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class MigrationIntegrationTest {

    private static final AtomicInteger PREFIX_COUNTER = new AtomicInteger();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Test
    void migratesAnEmptyDatabase() throws Exception {
        TableNames tables = new TableNames(uniquePrefix());
        try (DatabaseConnectionProvider provider = newProvider()) {
            new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();

            assertThat(tableExists(provider.dataSource(), tables.profiles())).isTrue();
            assertThat(tableExists(provider.dataSource(), tables.punishments())).isTrue();
            assertThat(tableExists(provider.dataSource(), tables.syncEvents())).isTrue();
            assertThat(schemaVersionRows(provider.dataSource(), tables)).containsExactly(1);
        }
    }

    @Test
    void migratingAnAlreadyUpToDateDatabaseIsANoOp() throws Exception {
        TableNames tables = new TableNames(uniquePrefix());
        try (DatabaseConnectionProvider provider = newProvider()) {
            SchemaMigrator migrator = new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t"));
            migrator.migrate();
            migrator.migrate();

            assertThat(schemaVersionRows(provider.dataSource(), tables)).containsExactly(1);
        }
    }

    @Test
    void threeSimultaneousInitializationsAllSucceedAndAgreeOnTheFinalVersion() throws Exception {
        TableNames tables = new TableNames(uniquePrefix());
        try (DatabaseConnectionProvider provider = newProvider()) {
            ExecutorService pool = Executors.newFixedThreadPool(3);
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                futures.add(pool.submit(() -> {
                    new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            pool.shutdown();

            assertThat(schemaVersionRows(provider.dataSource(), tables)).containsExactly(1);
        }
    }

    @Test
    void refusesToStartWhenTheStoredSchemaVersionIsNewerThanThisBinary() throws Exception {
        TableNames tables = new TableNames(uniquePrefix());
        try (DatabaseConnectionProvider provider = newProvider()) {
            new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();
            insertSchemaVersionRow(provider.dataSource(), tables, 999);

            assertThatThrownBy(() -> new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate())
                    .isInstanceOf(SchemaVersionTooNewException.class);
        }
    }

    private static DatabaseConnectionProvider newProvider() {
        return new DatabaseConnectionProvider(new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "", false,
                new PoolSettings(5, 1, 5_000, 60_000), 5_000));
    }

    private static String uniquePrefix() {
        return "mig" + PREFIX_COUNTER.incrementAndGet() + "_";
    }

    private static boolean tableExists(DataSource dataSource, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static List<Integer> schemaVersionRows(DataSource dataSource, TableNames tables) throws SQLException {
        List<Integer> versions = new java.util.ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT version FROM `" + tables.schemaVersion() + "` ORDER BY version")) {
            while (rs.next()) {
                versions.add(rs.getInt(1));
            }
        }
        return versions;
    }

    private static void insertSchemaVersionRow(DataSource dataSource, TableNames tables, int version) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `" + tables.schemaVersion() + "` (version, applied_at) VALUES (?, UTC_TIMESTAMP(3))")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }
}
