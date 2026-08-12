package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class IndexUsageIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider provider;
    private static TableNames tables;

    @BeforeAll
    static void setUp() throws Exception {
        provider = new DatabaseConnectionProvider(new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "", false,
                new PoolSettings(5, 1, 5_000, 60_000), 5_000));
        tables = new TableNames("");
        new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("index-usage-test")).migrate();
    }

    @AfterAll
    static void tearDown() {
        provider.close();
    }

    @Test
    void loginBanCheckUsesAnIndexOnTargetUuidNotAFullTableScan() throws Exception {
        String sql = "EXPLAIN SELECT * FROM `" + tables.punishments() + "` "
                + "WHERE target_uuid = UNHEX('00000000000000000000000000000000') AND category = 'BAN' "
                + "AND revoked_at IS NULL AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(3)) "
                + "ORDER BY created_at DESC LIMIT 1";
        assertThat(usedKey(sql)).isIn("idx_punishments_active", "idx_punishments_history");
    }

    @Test
    void staffAggregationUsesTheReportIndex() throws Exception {
        String sql = "EXPLAIN SELECT COUNT(*) FROM `" + tables.punishments() + "` "
                + "WHERE category = 'BAN' AND created_at <= '2030-01-01 00:00:00' "
                + "AND created_at >= '2000-01-01 00:00:00' "
                + "AND actor_uuid = UNHEX('00000000000000000000000000000000')";
        assertThat(usedKey(sql)).isEqualTo("idx_punishments_report");
    }

    private static String usedKey(String explainSql) throws Exception {
        try (Connection connection = provider.dataSource().getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(explainSql)) {
            rs.next();
            return rs.getString("key");
        }
    }
}
