package com.spunish.common.service;

import com.spunish.common.duration.DurationParser;
import com.spunish.common.duration.PunishmentDuration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A staffer holding more than one matching {@code spunish.limit.*} permission
 * gets the most permissive (largest) of them — more permissions should never
 * mean less capability. Holding none of the configured limits means no
 * restriction at all.
 */
public final class DurationLimitPolicy {

    private static final String PERMISSION_PREFIX = "spunish.limit.";

    private final DurationParser parser = new DurationParser();

    /**
     * @return empty when no configured limit applies — either no matching
     * permission, or a matching permission itself grants an unrestricted
     * ({@code n}) cap.
     */
    public Optional<PunishmentDuration> effectiveLimit(Map<String, String> configuredLimits, Predicate<String> hasPermission) {
        Optional<PunishmentDuration> max = Optional.empty();
        for (Map.Entry<String, String> entry : configuredLimits.entrySet()) {
            if (!hasPermission.test(PERMISSION_PREFIX + entry.getKey())) {
                continue;
            }
            Optional<PunishmentDuration> parsed = parser.parse(entry.getValue());
            if (parsed.isEmpty()) {
                continue;
            }
            if (parsed.get() instanceof PunishmentDuration.Permanent) {
                return Optional.empty();
            }
            if (max.isEmpty() || isLarger(parsed.get(), max.get())) {
                max = parsed;
            }
        }
        return max;
    }

    public boolean exceeds(PunishmentDuration requested, PunishmentDuration limit) {
        if (requested instanceof PunishmentDuration.Permanent) {
            return true;
        }
        if (limit instanceof PunishmentDuration.Permanent) {
            return false;
        }
        return isLarger(requested, limit);
    }

    private static boolean isLarger(PunishmentDuration a, PunishmentDuration b) {
        PunishmentDuration.Temporary left = (PunishmentDuration.Temporary) a;
        PunishmentDuration.Temporary right = (PunishmentDuration.Temporary) b;
        return left.duration().compareTo(right.duration()) > 0;
    }
}
