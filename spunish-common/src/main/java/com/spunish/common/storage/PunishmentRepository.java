package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentRepository {

    CompletableFuture<Punishment> insert(InsertPunishmentCommand command);

    /**
     * Revokes {@code previousPunishmentId} and inserts {@code newPunishment} inside a single
     * transaction: a failure applying the new punishment must never leave the target with the
     * old punishment revoked and nothing active in its place (design.md decision 1).
     */
    CompletableFuture<OverrideResult> insertWithOverride(
            InsertPunishmentCommand newPunishment, long previousPunishmentId, Actor revoker, Instant revokedAt, String revokeReason);

    /**
     * Whether a punishment is active is decided by the database's own clock, not the
     * caller's, so expiration is judged consistently regardless of any clock skew on the
     * calling server.
     */
    CompletableFuture<Optional<Punishment>> findActive(UUID targetUuid, PunishmentCategory category);

    CompletableFuture<Optional<Punishment>> findById(long id);

    CompletableFuture<Optional<Punishment>> findByPublicId(String publicId);

    /**
     * @return {@code true} if the punishment was revoked; {@code false} if it did not exist or
     * was already revoked, so callers can safely race to revoke the same punishment.
     */
    CompletableFuture<Boolean> revoke(long punishmentId, Actor revoker, Instant revokedAt, String revokeReason, String originServer);

    CompletableFuture<List<Punishment>> findHistory(
            UUID targetUuid, Optional<PunishmentCategory> category, int limit, int offset);
}
