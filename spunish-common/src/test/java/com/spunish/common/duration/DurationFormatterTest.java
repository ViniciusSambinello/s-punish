package com.spunish.common.duration;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DurationFormatterTest {

    private final DurationFormatter formatter = new DurationFormatter();

    private final DurationFormatSpec spec = new DurationFormatSpec(
            Map.of(
                    DurationUnit.SECOND, "s",
                    DurationUnit.MINUTE, "m",
                    DurationUnit.HOUR, "h",
                    DurationUnit.DAY, "d",
                    DurationUnit.WEEK, "w",
                    DurationUnit.MONTH, "mo",
                    DurationUnit.YEAR, "y"),
            2,
            "permanente");

    @Test
    void permanentDurationUsesConfiguredText() {
        assertThat(formatter.format((Duration) null, spec)).isEqualTo("permanente");
    }

    @Test
    void permanentPunishmentDurationUsesConfiguredText() {
        assertThat(formatter.format(PunishmentDuration.Permanent.INSTANCE, spec)).isEqualTo("permanente");
    }

    @Test
    void truncatesToConfiguredMaxUnits() {
        Duration remaining = Duration.ofDays(2).plusHours(3).plusMinutes(15);

        assertThat(formatter.format(remaining, spec)).isEqualTo("2d 3h");
    }

    @Test
    void showsAllComponentsWhenWithinMaxUnits() {
        DurationFormatSpec singleUnitSpec = new DurationFormatSpec(spec.unitLabels(), 1, spec.permanentText());

        assertThat(formatter.format(Duration.ofMinutes(90), singleUnitSpec)).isEqualTo("1h");
    }

    @Test
    void zeroDurationFallsBackToZeroSeconds() {
        assertThat(formatter.format(Duration.ZERO, spec)).isEqualTo("0s");
    }

    @Test
    void unlabeledUnitFallsBackToUnitCode() {
        DurationFormatSpec noLabels = new DurationFormatSpec(Map.of(), 2, "permanente");

        assertThat(formatter.format(Duration.ofDays(1), noLabels)).isEqualTo("1d");
    }
}
