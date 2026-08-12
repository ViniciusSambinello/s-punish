package com.spunish.common.domain;

import java.time.Instant;

/**
 * The exhaustive set of states a punishment can be in relative to a reference
 * instant. Sealed so every display and enforcement path is forced to handle
 * all three — the exact place where silently forgetting "expired" would let
 * a revoked-looking entry read as active.
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
     * background sweep — so correctness never depends on a periodic task having run.
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
