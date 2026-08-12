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
     * Never throws: a sync failure is logged and swallowed so it can never
     * disable the plugin or stop local enforcement, which does not depend on
     * this consumer succeeding.
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
