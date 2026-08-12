package com.spunish.common.duration;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Either permanent, or a fixed {@link Duration} applied from the moment the
 * punishment is created.
 */
public sealed interface PunishmentDuration permits PunishmentDuration.Permanent, PunishmentDuration.Temporary {

    /**
     * @return the expiration instant for a punishment applied at {@code appliedAt},
     * or empty when this duration is permanent.
     */
    Optional<Instant> expiresAt(Instant appliedAt);

    /**
     * Round-trips back to the {@code quantity+unit} syntax {@link DurationParser}
     * accepts (e.g. {@code 1y6mo}, or {@code n} for permanent) — used to offer a
     * reason's own default duration as a tab-complete suggestion for {@code <time>}.
     */
    default String canonicalText() {
        if (!(this instanceof Temporary temporary)) {
            return "n";
        }
        long remainingSeconds = Math.max(0, temporary.duration().getSeconds());
        StringBuilder text = new StringBuilder();
        for (DurationUnit unit : DurationUnit.DESCENDING) {
            long quantity = remainingSeconds / unit.seconds();
            if (quantity <= 0) {
                continue;
            }
            remainingSeconds -= quantity * unit.seconds();
            text.append(quantity).append(unit.code());
        }
        return text.isEmpty() ? "0s" : text.toString();
    }

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
