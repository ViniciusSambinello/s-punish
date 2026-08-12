package com.spunish.common.sync;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.DatabaseConnectionProvider;
import com.spunish.common.storage.IoExecutor;
import com.spunish.common.storage.InsertPunishmentCommand;
import com.spunish.common.storage.MySqlSyncEventRepository;
import com.spunish.common.storage.OverrideResult;
import com.spunish.common.storage.PunishmentRepository;
import com.spunish.common.storage.SchemaMigrator;
import com.spunish.common.storage.SyncEvent;
import com.spunish.common.storage.SyncEventType;
import com.spunish.common.storage.TableNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A transaction that grabs a lower AUTO_INCREMENT id can still commit — and so
 * become visible — after a transaction that grabbed a higher one; the sync
 * consumer must not lose that event.
 */
@Testcontainers
class SyncEventConsumerIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider connectionProvider;
    private static TableNames tables;

    @BeforeAll
    static void migrate() throws Exception {
        DatabaseSettings settings = new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "sp_", false,
                new PoolSettings(5, 1, 5_000, 60_000), 5_000);
        connectionProvider = new DatabaseConnectionProvider(settings);
        tables = new TableNames("sp_");
        new SchemaMigrator(connectionProvider.dataSource(), tables, Logger.getLogger("migrate")).migrate();
    }

    @AfterAll
    static void close() {
        connectionProvider.close();
    }

    @Test
    void noEventIsLostWhenALowerAutoIncrementIdCommitsAfterAHigherOne() throws Exception {
        Instant sharedCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UUID targetA = UUID.randomUUID();
        UUID targetB = UUID.randomUUID();
        long punishmentIdA = 1001;
        long punishmentIdB = 1002;

        MySqlSyncEventRepository syncEventRepository = new MySqlSyncEventRepository(
                connectionProvider.dataSource(), new IoExecutor(Duration.ofSeconds(10)), tables);

        long idA;
        long idB;
        try (Connection connA = connectionProvider.dataSource().getConnection();
                Connection connB = connectionProvider.dataSource().getConnection()) {
            connA.setAutoCommit(false);
            connB.setAutoCommit(false);

            idA = insertRawSyncEvent(connA, targetA, punishmentIdA, sharedCreatedAt);
            idB = insertRawSyncEvent(connB, targetB, punishmentIdB, sharedCreatedAt);
            connB.commit();
            assertThat(idB).isGreaterThan(idA);

            List<SyncEvent> whileAStillOpen = syncEventRepository.pollSince(sharedCreatedAt.minusSeconds(5)).get();
            assertThat(whileAStillOpen).extracting(SyncEvent::id).containsExactly(idB);

            connA.commit();
        }

        StubPunishmentLookup punishmentLookup = new StubPunishmentLookup();
        punishmentLookup.put(punishmentIdA, targetA);
        punishmentLookup.put(punishmentIdB, targetB);
        RecordingListener listener = new RecordingListener();
        FixedClock clock = new FixedClock(sharedCreatedAt.plusSeconds(2));

        SyncEventConsumer consumer = new SyncEventConsumer(
                syncEventRepository, punishmentLookup, (ServerIdentity) () -> "consumer-under-test",
                clock, listener, Logger.getLogger("consumer-test"), Duration.ofDays(1), 1_000);

        consumer.pollOnce().get();
        // Same overlap window on the next poll: B must not be re-delivered, but A must
        // still be delivered now that its commit has landed — the "no event lost" guarantee.
        consumer.pollOnce().get();

        assertThat(listener.deliveredPunishmentIds()).containsExactlyInAnyOrder(punishmentIdA, punishmentIdB);
    }

    private static long insertRawSyncEvent(Connection connection, UUID targetUuid, long punishmentId, Instant createdAt)
            throws Exception {
        String sql = "INSERT INTO `" + tables.syncEvents()
                + "` (type, punishment_id, target_uuid, origin_server, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, SyncEventType.PUNISHMENT_CREATED.name());
            statement.setLong(2, punishmentId);
            statement.setBytes(3, toBytes(targetUuid));
            statement.setString(4, "server-under-race");
            statement.setTimestamp(5, Timestamp.from(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static byte[] toBytes(UUID uuid) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private static final class FixedClock implements SystemClock {
        private final Instant instant;

        FixedClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant now() {
            return instant;
        }
    }

    private static final class RecordingListener implements SyncEventListener {
        private final java.util.Set<Long> delivered = ConcurrentHashMap.newKeySet();

        @Override
        public void onPunishmentCreated(Punishment punishment) {
            delivered.add(punishment.id());
        }

        @Override
        public void onPunishmentRevoked(Punishment punishment) {
            delivered.add(punishment.id());
        }

        java.util.Set<Long> deliveredPunishmentIds() {
            return delivered;
        }
    }

    private static final class StubPunishmentLookup implements PunishmentRepository {
        private final Map<Long, Punishment> byId = new ConcurrentHashMap<>();

        void put(long punishmentId, UUID targetUuid) {
            byId.put(punishmentId, new Punishment(
                    punishmentId, "PUB" + punishmentId, PunishmentCategory.MUTE, targetUuid, "Target",
                    ConsoleActor.INSTANCE, "spam", "Spam", Instant.parse("2026-01-01T00:00:00Z"),
                    null, "server-under-race", null, null, null));
        }

        @Override
        public java.util.concurrent.CompletableFuture<Optional<Punishment>> findById(long id) {
            return java.util.concurrent.CompletableFuture.completedFuture(Optional.ofNullable(byId.get(id)));
        }

        @Override
        public java.util.concurrent.CompletableFuture<Punishment> insert(InsertPunishmentCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<OverrideResult> insertWithOverride(
                InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Optional<Punishment>> findByPublicId(String publicId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> revoke(
                long punishmentId, Actor revoker, Instant revokedAt, String revokeReason, String originServer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.CompletableFuture<List<Punishment>> findHistory(
                UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset) {
            throw new UnsupportedOperationException();
        }
    }
}
