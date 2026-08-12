package com.spunish.common.duration;

import java.util.List;
import java.util.Optional;

/**
 * Units accepted by {@link DurationParser}. {@code MONTH} and {@code YEAR}
 * are fixed at 30 and 365 days — not calendar-accurate — since punishment
 * durations run from a fixed instant, not a calendar date.
 */
public enum DurationUnit {

    SECOND("s", 1L),
    MINUTE("m", 60L),
    HOUR("h", 3_600L),
    DAY("d", 86_400L),
    WEEK("w", 604_800L),
    MONTH("mo", 2_592_000L),
    YEAR("y", 31_536_000L);

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
