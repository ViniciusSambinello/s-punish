package com.spunish.common.duration;

import java.util.Map;
import java.util.Objects;

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
