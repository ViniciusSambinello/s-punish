package com.spunish.common.domain;

import java.time.Instant;

/**
 * A punishment is in exactly one of three states relative to a reference
 * instant: active, expired, or revoked.
 */
public sealed interface PunishmentState {

    Punishment punishment();

    record Active(Punishment punishment) implements PunishmentState {
    }

    record Expired(Punishment punishment) implements PunishmentState {
    }

    record Revoked(Punishment punishment) implements PunishmentState {
    }

    /**
     * Expiration is evaluated at read time against {@code reference}, never by a
     * background sweep — correctness never depends on a periodic task having run.
     */
    static PunishmentState resolve(Punishment punishment, Instant reference) {
        if (punishment.isRevoked()) {
            return new Revoked(punishment);
        }
        if (!punishment.isPermanent() && !punishment.expiresAt().isAfter(reference)) {
            return new Expired(punishment);
        }
        return new Active(punishment);
    }
}
