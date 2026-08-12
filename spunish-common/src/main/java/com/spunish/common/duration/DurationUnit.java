package com.spunish.common.duration;

import java.util.List;
import java.util.Optional;

/**
 * Units accepted by {@link DurationParser}. Seconds-per-unit for {@code MONTH}
 * and {@code YEAR} are fixed approximations (30 and 365 days) rather than
 * calendar-accurate spans — punishment durations are computed against a
 * fixed {@link java.time.Instant}, not a calendar date, so there is no
 * "current month length" to anchor to.
 */
public enum DurationUnit {

    SECOND("s", 1L),
    MINUTE("m", 60L),
    HOUR("h", 3_600L),
    DAY("d", 86_400L),
    WEEK("w", 604_800L),
    MONTH("mo", 2_592_000L),
    YEAR("y", 31_536_000L);

    /**
     * Largest unit first, for greedy decomposition when formatting a duration back to text.
     */
    public static final List<DurationUnit> DESCENDING = List.of(YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND);

    private final String code;
    private final long seconds;

    DurationUnit(String code, long seconds) {
        this.code = code;
        this.seconds = seconds;
    }

    public String code() {
        return code;
    }

    public long seconds() {
        return seconds;
    }

    public static Optional<DurationUnit> fromCode(String code) {
        for (DurationUnit unit : values()) {
            if (unit.code.equals(code)) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }
}
