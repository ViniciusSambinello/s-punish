package com.spunish.common.duration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a duration as human text, showing at most
 * {@link DurationFormatSpec#maxUnits()} units, largest first.
 */
public final class DurationFormatter {

    /**
     * @param remaining {@code null} means permanent.
     */
    public String format(Duration remaining, DurationFormatSpec spec) {
        if (remaining == null) {
            return spec.permanentText();
        }

        long totalSeconds = Math.max(0, remaining.getSeconds());
        List<String> parts = new ArrayList<>();
        for (DurationUnit unit : DurationUnit.DESCENDING) {
            if (parts.size() == spec.maxUnits()) {
                break;
            }
            long quantity = totalSeconds / unit.seconds();
            if (quantity <= 0) {
                continue;
            }
            totalSeconds -= quantity * unit.seconds();
            parts.add(quantity + spec.labelFor(unit));
        }

        if (parts.isEmpty()) {
            return "0" + spec.labelFor(DurationUnit.SECOND);
        }
        return String.join(" ", parts);
    }

    public String format(PunishmentDuration duration, DurationFormatSpec spec) {
        if (duration instanceof PunishmentDuration.Temporary temporary) {
            return format(temporary.duration(), spec);
        }
        return spec.permanentText();
    }
}
