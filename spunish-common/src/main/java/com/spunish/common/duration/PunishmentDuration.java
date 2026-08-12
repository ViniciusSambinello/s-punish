package com.spunish.common.duration;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of interpreting a reason's or command's duration argument:
 * either permanent, or a fixed {@link Duration} applied from the moment the
 * punishment is created.
 */
public sealed interface PunishmentDuration permits PunishmentDuration.Permanent, PunishmentDuration.Temporary {

    /**
     * @return the expiration instant for a punishment applied at {@code appliedAt},
     * or empty when this duration is permanent.
     */
    Optional<Instant> expiresAt(Instant appliedAt);

    record Permanent() implements PunishmentDuration {

        public static final Permanent INSTANCE = new Permanent();

        @Override
        public Optional<Instant> expiresAt(Instant appliedAt) {
            return Optional.empty();
        }
    }

    record Temporary(Duration duration) implements PunishmentDuration {

        public Temporary {
            Objects.requireNonNull(duration, "duration");
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive");
            }
        }

        @Override
        public Optional<Instant> expiresAt(Instant appliedAt) {
            return Optional.of(appliedAt.plus(duration));
        }
    }
}
