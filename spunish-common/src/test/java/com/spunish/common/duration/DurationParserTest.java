package com.spunish.common.duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DurationParserTest {

    private final DurationParser parser = new DurationParser();

    @Test
    void parsesPermanentLiteral() {
        Optional<PunishmentDuration> result = parser.parse("n");

        assertThat(result).contains(PunishmentDuration.Permanent.INSTANCE);
    }

    @Test
    void parsesSingleUnit() {
        Optional<PunishmentDuration> result = parser.parse("30d");

        assertThat(result).contains(new PunishmentDuration.Temporary(Duration.ofDays(30)));
    }

    @Test
    void parsesCompositeDurationHoursAndMinutes() {
        Optional<PunishmentDuration> result = parser.parse("1h30m");

        assertThat(result).contains(new PunishmentDuration.Temporary(Duration.ofMinutes(90)));
    }

    @Test
    void parsesCompositeDurationYearsAndMonths() {
        Optional<PunishmentDuration> result = parser.parse("1y6mo");

        Duration expected = Duration.ofSeconds(31_536_000L + 6 * 2_592_000L);
        assertThat(result).contains(new PunishmentDuration.Temporary(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"30x", "abc", "1h ", " 1h", "1", "h1", "1hh"})
    void rejectsMalformedInput(String input) {
        assertThat(parser.parse(input)).isEmpty();
    }

    @Test
    void rejectsUnknownUnit() {
        assertThat(parser.parse("7x")).isEmpty();
    }

    @Test
    void rejectsZero() {
        assertThat(parser.parse("0s")).isEmpty();
    }

    @Test
    void rejectsZeroEvenWhenComposite() {
        assertThat(parser.parse("0s0m")).isEmpty();
    }

    @Test
    void rejectsEmpty() {
        assertThat(parser.parse("")).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void distinguishesMonthFromMinutePlusDanglingLetter() {
        Optional<PunishmentDuration> result = parser.parse("1mo");

        assertThat(result).contains(new PunishmentDuration.Temporary(Duration.ofSeconds(2_592_000L)));
    }
}
