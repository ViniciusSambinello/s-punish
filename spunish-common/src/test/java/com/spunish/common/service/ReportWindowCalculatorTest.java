package com.spunish.common.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWindowCalculatorTest {

    private final ReportWindowCalculator calculator = new ReportWindowCalculator();

    @Test
    void dailyWindowCutsAtMidnightInTheConfiguredZoneNotUtc() {
        // 2026-01-02T01:30:00Z is already 2026-01-01T22:30:00-03:00 the previous
        // day in America/Sao_Paulo — a UTC-midnight cutoff would wrongly place
        // the window's start 3 hours into "today" UTC.
        Instant now = Instant.parse("2026-01-02T01:30:00Z");
        ZoneId saoPaulo = ZoneOffset.ofHours(-3);

        WindowBounds bounds = calculator.bounds(ReportWindow.DAILY, now, saoPaulo);

        assertThat(bounds.from()).isEqualTo(Instant.parse("2026-01-01T03:00:00Z"));
        assertThat(bounds.to()).isEqualTo(now);
    }

    @Test
    void dailyWindowInUtcCutsAtUtcMidnight() {
        Instant now = Instant.parse("2026-01-02T10:00:00Z");

        WindowBounds bounds = calculator.bounds(ReportWindow.DAILY, now, ZoneOffset.UTC);

        assertThat(bounds.from()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void weeklyWindowIsSevenRollingDays() {
        Instant now = Instant.parse("2026-01-08T12:00:00Z");

        WindowBounds bounds = calculator.bounds(ReportWindow.WEEKLY, now, ZoneOffset.UTC);

        assertThat(bounds.from()).isEqualTo(now.minus(Duration.ofDays(7)));
    }

    @Test
    void monthlyWindowIsThirtyRollingDays() {
        Instant now = Instant.parse("2026-01-31T12:00:00Z");

        WindowBounds bounds = calculator.bounds(ReportWindow.MONTHLY, now, ZoneOffset.UTC);

        assertThat(bounds.from()).isEqualTo(now.minus(Duration.ofDays(30)));
    }

    @Test
    void allTimeWindowHasNoLowerBound() {
        Instant now = Instant.parse("2026-01-31T12:00:00Z");

        WindowBounds bounds = calculator.bounds(ReportWindow.ALL_TIME, now, ZoneOffset.UTC);

        assertThat(bounds.from()).isNull();
        assertThat(bounds.to()).isEqualTo(now);
    }
}
