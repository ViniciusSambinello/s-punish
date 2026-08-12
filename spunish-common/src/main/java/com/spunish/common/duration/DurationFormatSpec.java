package com.spunish.common.duration;

import java.util.Map;
import java.util.Objects;

/**
 * The pieces of {@code messages.yml} that {@link DurationFormatter} needs.
 * Kept as a plain value here so the domain module stays free of any
 * dependency on the Configurate-backed configuration model in
 * {@code spunish-common}'s config package.
 */
public record DurationFormatSpec(Map<DurationUnit, String> unitLabels, int maxUnits, String permanentText) {

    public DurationFormatSpec {
        Objects.requireNonNull(unitLabels, "unitLabels");
        Objects.requireNonNull(permanentText, "permanentText");
        if (maxUnits <= 0) {
            throw new IllegalArgumentException("maxUnits must be positive");
        }
        unitLabels = Map.copyOf(unitLabels);
    }

    public String labelFor(DurationUnit unit) {
        return unitLabels.getOrDefault(unit, unit.code());
    }
}
