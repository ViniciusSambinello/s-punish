package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The persisted gap between {@code createdAt} and {@code expiresAt} always matches the
 * configured punishment duration exactly, unaffected by any clock skew against the
 * database server.
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
