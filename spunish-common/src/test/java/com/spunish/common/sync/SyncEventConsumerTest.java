package com.spunish.common.sync;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.InsertPunishmentCommand;
import com.spunish.common.storage.OverrideResult;
import com.spunish.common.storage.PunishmentRepository;
import com.spunish.common.storage.SyncEvent;
import com.spunish.common.storage.SyncEventRepository;
import com.spunish.common.storage.SyncEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class SyncEventConsumerTest {

    private FakeSyncEventRepository events;
    private FakePunishmentLookup punishments;
    private RecordingListener listener;
    private FixedClock clock;
    private SyncEventConsumer consumer;

    private final UUID targetUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        events = new FakeSyncEventRepository();
        punishments = new FakePunishmentLookup();
        listener = new RecordingListener();
        clock = new FixedClock(Instant.parse("2026-01-01T00:00:10Z"));
        consumer = new SyncEventConsumer(
                events, punishments, (ServerIdentity) () -> "this-server", clock, listener,
                Logger.getLogger("SyncEventConsumerTest"), Duration.ofSeconds(1), 100);
    }

    private static Punishment punishment(long id) {
        return new Punishment(id, "PUB" + id, PunishmentCategory.MUTE, UUID.randomUUID(), "Target",
                ConsoleActor.INSTANCE, "spam", "Spam", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"), "other-server", null, null, null);
    }

    @Test
    void dispatchesCreatedEventWithFullyResolvedPunishment() throws Exception {
        Punishment p = punishment(1);
        punishments.put(p);
        events.nextBatch = List.of(new SyncEvent(1, SyncEventType.PUNISHMENT_CREATED, 1, targetUuid, "other-server", clock.now()));

        consumer.pollOnce().get();

        assertThat(listener.created).containsExactly(p);
        assertThat(listener.revoked).isEmpty();
    }

    @Test
    void dispatchesRevokedEventWithFullyResolvedPunishment() throws Exception {
        Punishment p = punishment(1);
        punishments.put(p);
        events.nextBatch = List.of(new SyncEvent(2, SyncEventType.PUNISHMENT_REVOKED, 1, targetUuid, "other-server", clock.now()));

        consumer.pollOnce().get();

        assertThat(listener.revoked).containsExactly(p);
        assertThat(listener.created).isEmpty();
    }

    @Test
    void selfOriginatedEventsAreIgnored() throws Exception {
        punishments.put(punishment(1));
        events.nextBatch = List.of(new SyncEvent(1, SyncEventType.PUNISHMENT_CREATED, 1, targetUuid, "this-server", clock.now()));

        consumer.pollOnce().get();

        assertThat(listener.created).isEmpty();
    }

    @Test
    void sameEventIdIsNotDispatchedTwiceAcrossOverlappingPolls() throws Exception {
        punishments.put(punishment(1));
        SyncEvent event = new SyncEvent(1, SyncEventType.PUNISHMENT_CREATED, 1, targetUuid, "other-server", clock.now());
        events.nextBatch = List.of(event);

        consumer.pollOnce().get();
        // Overlap window means the next poll may legitimately re-fetch the same event.
        events.nextBatch = List.of(event);
        consumer.pollOnce().get();

        assertThat(listener.created).hasSize(1);
    }

    @Test
    void missingPunishmentIsSkippedWithoutError() throws Exception {
        events.nextBatch = List.of(new SyncEvent(1, SyncEventType.PUNISHMENT_CREATED, 999, targetUuid, "other-server", clock.now()));

        consumer.pollOnce().get();

        assertThat(listener.created).isEmpty();
    }

    @Test
    void failedPollDoesNotAdvanceTheCursorAndRetriesTheSameWindow() throws Exception {
        events.failNext = true;

        consumer.pollOnce().get(); // a failed poll must not crash the consumer; the failure is swallowed
        Instant pollFromAfterFailure = events.lastRequestedSince;

        events.failNext = false;
        events.nextBatch = List.of();
        consumer.pollOnce().get();
        Instant pollFromAfterRetry = events.lastRequestedSince;

        assertThat(pollFromAfterRetry).isEqualTo(pollFromAfterFailure);
        assertThat(listener.created).isEmpty();
    }

    private static final class FixedClock implements SystemClock {
        private Instant instant;

        FixedClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant now() {
            return instant;
        }
    }

    private static final class RecordingListener implements SyncEventListener {
        final List<Punishment> created = new ArrayList<>();
        final List<Punishment> revoked = new ArrayList<>();

        @Override
        public void onPunishmentCreated(Punishment punishment) {
            created.add(punishment);
        }

        @Override
        public void onPunishmentRevoked(Punishment punishment) {
            revoked.add(punishment);
        }
    }

    private static final class FakeSyncEventRepository implements SyncEventRepository {
        List<SyncEvent> nextBatch = List.of();
        boolean failNext;
        Instant lastRequestedSince;

        @Override
        public CompletableFuture<List<SyncEvent>> pollSince(Instant sinceInclusive) {
            lastRequestedSince = sinceInclusive;
            if (failNext) {
                failNext = false;
                CompletableFuture<List<SyncEvent>> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("storage unavailable"));
                return failed;
            }
            return CompletableFuture.completedFuture(nextBatch);
        }

        @Override
        public CompletableFuture<Integer> deleteOlderThan(Instant threshold) {
            return CompletableFuture.completedFuture(0);
        }
    }

    private static final class FakePunishmentLookup implements PunishmentRepository {
        private final Map<Long, Punishment> byId = new ConcurrentHashMap<>();

        void put(Punishment punishment) {
            byId.put(punishment.id(), punishment);
        }

        @Override
        public CompletableFuture<Optional<Punishment>> findById(long id) {
            return CompletableFuture.completedFuture(Optional.ofNullable(byId.get(id)));
        }

        @Override
        public CompletableFuture<Punishment> insert(InsertPunishmentCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<OverrideResult> insertWithOverride(
                InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Optional<Punishment>> findByPublicId(String publicId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> revoke(
                long punishmentId, Actor revoker, Instant revokedAt, String revokeReason, String originServer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<List<Punishment>> findHistory(
                UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset) {
            throw new UnsupportedOperationException();
        }
    }
}
