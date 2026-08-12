package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * @param createdAt and {@code expiresAt} are computed by the caller from a single
 *                   {@code SystemClock.now()} reading (see {@code PunishmentDuration.expiresAt}) —
 *                   never re-derived here — so the persisted gap between them
 *                   always matches the configured duration exactly, regardless
 *                   of any clock skew against the database server.
 */
public record InsertPunishmentCommand(
        PunishmentCategory category,
        UUID targetUuid,
        String targetName,
        Actor actor,
        String reasonId,
        String reasonDisplay,
        Instant createdAt,
        Instant expiresAt,
        String originServer) {

    public InsertPunishmentCommand {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(targetUuid, "targetUuid");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(reasonId, "reasonId");
        Objects.requireNonNull(reasonDisplay, "reasonDisplay");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(originServer, "originServer");
    }
}
