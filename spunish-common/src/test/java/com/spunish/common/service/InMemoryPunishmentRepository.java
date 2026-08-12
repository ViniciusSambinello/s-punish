package com.spunish.common.service;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.storage.InsertPunishmentCommand;
import com.spunish.common.storage.OverrideResult;
import com.spunish.common.storage.PunishmentRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory fake for the precedence-rule tests (task 5.7/5.9) — the interface
 * exists precisely so these don't need a database. {@code findActive} checks
 * expiry against the same injected {@link SystemClock} the test controls
 * (unlike production, which anchors to the database's own clock) — using
 * real wall-clock time here would make expiry depend on which real-world
 * date the test happens to run on.
 */
final class InMemoryPunishmentRepository implements PunishmentRepository {

    private final java.util.Map<Long, Punishment> byId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();
    private final SystemClock clock;

    InMemoryPunishmentRepository(SystemClock clock) {
        this.clock = clock;
    }

    @Override
    public CompletableFuture<Punishment> insert(InsertPunishmentCommand command) {
        return CompletableFuture.completedFuture(insertNow(command));
    }

    @Override
    public CompletableFuture<OverrideResult> insertWithOverride(
            InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason) {
        Punishment revoked = revokeNow(previousPunishmentId, revoker, revokedAt, revokeReason);
        if (revoked == null) {
            throw new IllegalStateException("Punishment " + previousPunishmentId + " was already revoked or does not exist");
        }
        Punishment applied = insertNow(newPunishment);
        return CompletableFuture.completedFuture(new OverrideResult(applied, revoked));
    }

    private Punishment insertNow(InsertPunishmentCommand command) {
        long id = idSequence.incrementAndGet();
        Punishment punishment = new Punishment(
                id, "TEST" + id, command.category(), command.targetUuid(), command.targetName(),
                command.actor(), command.reasonId(), command.reasonDisplay(), command.createdAt(),
                command.expiresAt(), command.originServer(), null, null, null);
        byId.put(id, punishment);
        return punishment;
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category) {
        Instant now = clock.now();
        return CompletableFuture.completedFuture(byId.values().stream()
                .filter(p -> p.targetUuid().equals(targetUuid) && p.category() == category)
                .filter(p -> !p.isRevoked())
                .filter(p -> p.isPermanent() || p.expiresAt().isAfter(now))
                .max(Comparator.comparing(Punishment::createdAt)));
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findById(long id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(byId.get(id)));
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findByPublicId(String publicId) {
        return CompletableFuture.completedFuture(
                byId.values().stream().filter(p -> p.publicId().equals(publicId)).findFirst());
    }

    @Override
    public CompletableFuture<Boolean> revoke(
            long punishmentId, Actor revoker, Instant revokedAt, String revokeReason, String originServer) {
        return CompletableFuture.completedFuture(revokeNow(punishmentId, revoker, revokedAt, revokeReason) != null);
    }

    /** @return the revoked punishment, or {@code null} if it was missing/already revoked. */
    private Punishment revokeNow(long punishmentId, Actor revoker, Instant revokedAt, String revokeReason) {
        Punishment existing = byId.get(punishmentId);
        if (existing == null || existing.isRevoked()) {
            return null;
        }
        Punishment revoked = new Punishment(
                existing.id(), existing.publicId(), existing.category(), existing.targetUuid(), existing.targetName(),
                existing.actor(), existing.reasonId(), existing.reasonDisplay(), existing.createdAt(),
                existing.expiresAt(), existing.originServer(), revoker, revokedAt, revokeReason);
        byId.put(punishmentId, revoked);
        return revoked;
    }

    @Override
    public CompletableFuture<List<Punishment>> findHistory(
            UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset) {
        List<Punishment> filtered = byId.values().stream()
                .filter(p -> p.targetUuid().equals(targetUuid))
                .filter(p -> category.isEmpty() || p.category() == category.get())
                .sorted(Comparator.comparing(Punishment::createdAt).reversed())
                .skip(offset)
                .limit(limit)
                .toList();
        return CompletableFuture.completedFuture(filtered);
    }
}
