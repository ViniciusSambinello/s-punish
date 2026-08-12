package com.spunish.common.sync;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.PunishmentRepository;
import com.spunish.common.storage.SyncEvent;
import com.spunish.common.storage.SyncEventRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Polls {@code sync_events} with an overlap window, deduplicates by id
 * against a bounded set of recently-seen ids, ignores events this instance
 * itself produced, and resolves each surviving event to its full
 * {@link Punishment} before handing it to a {@link SyncEventListener}.
 *
 * The overlap exists because {@code AUTO_INCREMENT} is assigned at insert
 * time but a row only becomes visible at commit — a transaction that grabbed
 * id 100 can commit after one that grabbed 101, so filtering strictly by
 * "id greater than the last one seen" can permanently miss 100. Filtering by
 * {@code created_at} with an overlap and deduplicating by id closes that
 * window instead (design.md decision 8).
 */
public final class SyncEventConsumer {

    private final SyncEventRepository syncEventRepository;
    private final PunishmentRepository punishmentRepository;
    private final ServerIdentity serverIdentity;
    private final SystemClock clock;
    private final SyncEventListener listener;
    private final Logger logger;
    private final Duration overlap;

    private final RecentIds recentIds;
    private volatile Instant lastPollInstant;

    public SyncEventConsumer(
            SyncEventRepository syncEventRepository,
            PunishmentRepository punishmentRepository,
            ServerIdentity serverIdentity,
            SystemClock clock,
            SyncEventListener listener,
            Logger logger,
            Duration overlap,
            int recentIdCapacity) {
        this.syncEventRepository = syncEventRepository;
        this.punishmentRepository = punishmentRepository;
        this.serverIdentity = serverIdentity;
        this.clock = clock;
        this.listener = listener;
        this.logger = logger;
        this.overlap = overlap;
        this.recentIds = new RecentIds(recentIdCapacity);
        this.lastPollInstant = clock.now();
    }

    /**
     * The poll cursor only advances on success — a failed poll is retried
     * from the same starting point next time (task 6.6), so no event is
     * skipped because of a transient failure. Never throws: a failure is
     * logged and swallowed so it can't disable the plugin or stop local
     * enforcement, which never depended on this consumer succeeding.
     */
    public CompletableFuture<Void> pollOnce() {
        Instant pollFrom = lastPollInstant.minus(overlap);
        return syncEventRepository.pollSince(pollFrom)
                .thenCompose(this::processEvents)
                .thenRun(() -> lastPollInstant = clock.now())
                .exceptionally(ex -> {
                    logger.log(Level.WARNING, "Sync event consumption failed; will retry on the next poll", unwrap(ex));
                    return null;
                });
    }

    private CompletableFuture<Void> processEvents(List<SyncEvent> events) {
        List<CompletableFuture<Void>> pending = events.stream()
                .filter(event -> recentIds.addIfAbsent(event.id()))
                .filter(event -> !serverIdentity.id().equals(event.originServer()))
                .map(this::dispatch)
                .toList();
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> dispatch(SyncEvent event) {
        return punishmentRepository.findById(event.punishmentId()).thenAccept(found -> found.ifPresent(punishment -> {
            switch (event.type()) {
                case PUNISHMENT_CREATED -> listener.onPunishmentCreated(punishment);
                case PUNISHMENT_REVOKED -> listener.onPunishmentRevoked(punishment);
            }
        }));
    }

    private static Throwable unwrap(Throwable ex) {
        return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
    }

    /**
     * Bounded FIFO membership set — {@code removeEldestEntry} makes a
     * {@link LinkedHashMap} evict its oldest entry once over capacity,
     * which is exactly the "limited set of recent ids" design.md decision 8
     * calls for, without unbounded growth over a long uptime.
     */
    private static final class RecentIds {

        private final int capacity;
        private final Map<Long, Boolean> ids;

        RecentIds(int capacity) {
            this.capacity = capacity;
            this.ids = new LinkedHashMap<>(capacity, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > RecentIds.this.capacity;
                }
            };
        }

        synchronized boolean addIfAbsent(long id) {
            return ids.put(id, Boolean.TRUE) == null;
        }
    }
}
