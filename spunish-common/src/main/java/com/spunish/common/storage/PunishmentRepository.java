package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A single MySQL implementation backs this in production ({@link MySqlPunishmentRepository});
 * the interface exists so precedence-rule tests (task 5.7) can run against an
 * in-memory fake without a database.
 */
public interface PunishmentRepository {

    CompletableFuture<Punishment> insert(InsertPunishmentCommand command);

    /**
     * Revokes {@code previousPunishmentId} and inserts {@code newPunishment}
     * inside a single database transaction (design.md decision 1 /
     * task 5.3's {@code spunish.punish.override} path) — a failure applying
     * the new punishment must not leave the target with the old one revoked
     * and nothing active in its place.
     */
    CompletableFuture<OverrideResult> insertWithOverride(
            InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason);

    /**
     * Active is decided by the database's own clock, not the caller's — see
     * {@code punishment/enforcement}'s expiration requirement. No reference
     * instant is accepted here on purpose.
     */
    CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category);

    CompletableFuture<Optional<Punishment>> findById(long id);

    CompletableFuture<Optional<Punishment>> findByPublicId(String publicId);

    /**
     * @return {@code true} if a row was actually revoked; {@code false} if the
     * punishment did not exist or was already revoked (caller races safely).
     */
    CompletableFuture<Boolean> revoke(long punishmentId, Actor revoker, Instant revokedAt, String revokeReason);

    /**
     * Plain limit/offset, not "page number" — callers that over-fetch by one
     * to detect a next page (see {@code PunishmentHistoryService}) would
     * otherwise have their offset silently corrupted by the padded page size.
     */
    CompletableFuture<List<Punishment>> findHistory(
            UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset);
}
