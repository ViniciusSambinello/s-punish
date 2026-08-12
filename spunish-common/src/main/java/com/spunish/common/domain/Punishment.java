package com.spunish.common.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code expiresAt == null} means the punishment is permanent;
 * {@code revokedAt == null} means the original application still stands.
 * {@code reasonDisplay} must keep showing the text that was in force when the
 * punishment was applied, even if the reason catalog later renames or removes
 * that reason.
 */
public record Punishment(
        long id,
        String publicId,
        PunishmentCategory category,
        UUID targetUuid,
        String targetName,
        Actor actor,
        String reasonId,
        String reasonDisplay,
        Instant createdAt,
        Instant expiresAt,
        String originServer,
        Actor revoker,
        Instant revokedAt,
        String revokeReason) {

    public Punishment {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(targetUuid, "targetUuid");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(reasonId, "reasonId");
        Objects.requireNonNull(reasonDisplay, "reasonDisplay");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(originServer, "originServer");
        if ((revoker == null) != (revokedAt == null)) {
            throw new IllegalArgumentException("revoker and revokedAt must both be null or both be set");
        }
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
