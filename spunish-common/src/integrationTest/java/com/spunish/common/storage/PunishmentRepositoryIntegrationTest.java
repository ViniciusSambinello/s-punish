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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PunishmentRepositoryIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider provider;
    private static IoExecutor ioExecutor;
    private static MySqlPunishmentRepository repository;

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
        repository = new MySqlPunishmentRepository(provider.dataSource(), ioExecutor, tables, syncEvents);
    }

    @AfterAll
    static void tearDown() {
        ioExecutor.close();
        provider.close();
    }

    @Test
    void insertPersistsTheRecordAndGeneratesAnEightCharacterPublicId() throws Exception {
        Punishment applied = repository.insert(command(
                PunishmentCategory.BAN, UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600))).get();

        assertThat(applied.id()).isPositive();
        assertThat(applied.publicId()).hasSize(8);
        assertThat(applied.category()).isEqualTo(PunishmentCategory.BAN);
        assertThat(applied.isRevoked()).isFalse();
    }

    @Test
    void findActiveReturnsAnUnexpiredUnrevokedPunishment() throws Exception {
        UUID target = UUID.randomUUID();
        Punishment applied = repository.insert(command(
                PunishmentCategory.BAN, target, Instant.now(), Instant.now().plusSeconds(3600))).get();

        Optional<Punishment> active = repository.findActive(target, PunishmentCategory.BAN).get();

        assertThat(active).isPresent();
        assertThat(active.get().id()).isEqualTo(applied.id());
    }

    @Test
    void findActiveDoesNotReturnAPunishmentAlreadyExpiredByTheDatabaseClock() throws Exception {
        UUID target = UUID.randomUUID();
        Instant past = Instant.now().minusSeconds(120);
        repository.insert(command(PunishmentCategory.BAN, target, past, past.plusSeconds(1))).get();

        Optional<Punishment> active = repository.findActive(target, PunishmentCategory.BAN).get();

        assertThat(active).isEmpty();
    }

    @Test
    void revokeClosesThePunishmentWithoutAlteringTheOriginalFields() throws Exception {
        UUID target = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        Punishment applied = repository.insert(command(PunishmentCategory.MUTE, target, createdAt, null)).get();

        boolean revoked = repository.revoke(applied.id(), ConsoleActor.INSTANCE, Instant.now(), "appeal accepted", "server-a").get();

        assertThat(revoked).isTrue();
        Optional<Punishment> reloaded = repository.findById(applied.id()).get();
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().isRevoked()).isTrue();
        assertThat(reloaded.get().revoker()).isEqualTo(ConsoleActor.INSTANCE);
        assertThat(reloaded.get().revokeReason()).isEqualTo("appeal accepted");
        assertThat(reloaded.get().reasonId()).isEqualTo(applied.reasonId());
        assertThat(reloaded.get().createdAt()).isEqualTo(applied.createdAt());
        assertThat(repository.findActive(target, PunishmentCategory.MUTE).get()).isEmpty();
    }

    @Test
    void findHistoryPaginatesInDescendingApplicationOrderWithoutSkippingOrRepeatingRows() throws Exception {
        UUID target = UUID.randomUUID();
        Instant base = Instant.now().minusSeconds(60);
        for (int i = 0; i < 5; i++) {
            repository.insert(command(PunishmentCategory.BAN, target, base.plusSeconds(i), base.plusSeconds(3600))).get();
        }

        List<Punishment> firstPage = repository.findHistory(target, Optional.empty(), 2, 0).get();
        List<Punishment> secondPage = repository.findHistory(target, Optional.empty(), 2, 2).get();
        List<Punishment> thirdPage = repository.findHistory(target, Optional.empty(), 2, 4).get();

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(2);
        assertThat(thirdPage).hasSize(1);
        assertThat(firstPage.get(0).createdAt()).isAfter(firstPage.get(1).createdAt());
        assertThat(firstPage.get(1).createdAt()).isAfter(secondPage.get(0).createdAt());

        List<Long> allIds = new java.util.ArrayList<>();
        firstPage.forEach(p -> allIds.add(p.id()));
        secondPage.forEach(p -> allIds.add(p.id()));
        thirdPage.forEach(p -> allIds.add(p.id()));
        assertThat(allIds).doesNotHaveDuplicates().hasSize(5);
    }

    private static InsertPunishmentCommand command(PunishmentCategory category, UUID target, Instant createdAt, Instant expiresAt) {
        return new InsertPunishmentCommand(
                category, target, "Target", ConsoleActor.INSTANCE, "reason", "Reason", createdAt, expiresAt, "server-a");
    }
}
