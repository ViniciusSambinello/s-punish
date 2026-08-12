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
        // Daily windows cut at midnight in the configured zone, not UTC midnight:
        // 2026-01-02T01:30:00Z is still 2026-01-01 in America/Sao_Paulo (UTC-3).
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
