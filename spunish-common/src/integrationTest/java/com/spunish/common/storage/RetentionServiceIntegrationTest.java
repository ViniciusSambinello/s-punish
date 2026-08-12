package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RetentionServiceIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider provider;
    private static IoExecutor ioExecutor;
    private static MySqlPunishmentRepository punishmentRepository;
    private static RetentionService retentionService;

    @BeforeAll
    static void setUp() throws Exception {
        provider = new DatabaseConnectionProvider(new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "", false,
                new PoolSettings(10, 2, 5_000, 60_000), 5_000));
        TableNames tables = new TableNames("");
        new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();
        ioExecutor = new IoExecutor(Duration.ofSeconds(10));
        MySqlSyncEventRepository syncEvents = new MySqlSyncEventRepository(provider.dataSource(), ioExecutor, tables);
        punishmentRepository = new MySqlPunishmentRepository(provider.dataSource(), ioExecutor, tables, syncEvents);
        retentionService = new RetentionService(provider.dataSource(), ioExecutor, tables);
    }

    @AfterAll
    static void tearDown() {
        ioExecutor.close();
        provider.close();
    }

    @Test
    void activePunishmentsAreNeverPurgedNoMatterHowOldTheyAre() throws Exception {
        Instant longAgo = Instant.now().minus(3650, ChronoUnit.DAYS);
        Punishment active = punishmentRepository.insert(new InsertPunishmentCommand(
                PunishmentCategory.BAN, UUID.randomUUID(), "Active", ConsoleActor.INSTANCE,
                "reason", "Reason", longAgo, null, "server-a")).get();

        int purged = retentionService.purgeClosedOlderThan(1).get();

        assertThat(purged).isZero();
        assertThat(punishmentRepository.findById(active.id()).get()).isPresent();
    }

    @Test
    void aRevokedPunishmentOlderThanTheRetentionWindowIsPurged() throws Exception {
        Punishment applied = punishmentRepository.insert(new InsertPunishmentCommand(
                PunishmentCategory.MUTE, UUID.randomUUID(), "Old", ConsoleActor.INSTANCE,
                "reason", "Reason", Instant.now().minus(400, ChronoUnit.DAYS), null, "server-a")).get();
        punishmentRepository.revoke(applied.id(), ConsoleActor.INSTANCE,
                Instant.now().minus(400, ChronoUnit.DAYS), null, "server-a").get();

        int purged = retentionService.purgeClosedOlderThan(365).get();

        assertThat(purged).isGreaterThanOrEqualTo(1);
        assertThat(punishmentRepository.findById(applied.id()).get()).isEmpty();
    }

    @Test
    void aClosedPunishmentWithinTheRetentionWindowIsKept() throws Exception {
        Instant recentlyExpired = Instant.now().minusSeconds(60);
        Punishment applied = punishmentRepository.insert(new InsertPunishmentCommand(
                PunishmentCategory.MUTE, UUID.randomUUID(), "Recent", ConsoleActor.INSTANCE,
                "reason", "Reason", recentlyExpired.minusSeconds(10), recentlyExpired, "server-a")).get();

        int purged = retentionService.purgeClosedOlderThan(365).get();

        assertThat(purged).isZero();
        assertThat(punishmentRepository.findById(applied.id()).get()).isPresent();
    }
}
