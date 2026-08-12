package com.spunish.common.service;

import com.spunish.common.config.ReportSettings;
import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    private CountingReportRepository repository;
    private FixedSystemClock clock;
    private ReportSettings settings;
    private ReportService service;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new CountingReportRepository();
        clock = new FixedSystemClock(Instant.parse("2026-01-01T00:00:00Z"));
        settings = new ReportSettings(5_000, 30_000, 10);
        service = new ReportService(repository, clock, () -> ZoneOffset.UTC, () -> settings);
    }

    @Test
    void firstRequestRunsAFreshAggregation() throws Exception {
        ReportOutcome<GeneralReportSummary> outcome =
                service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();

        assertThat(outcome).isInstanceOf(ReportOutcome.Ready.class);
        assertThat(repository.aggregationCalls.get()).isEqualTo(1);
    }

    @Test
    void secondRequestWithinCacheDurationServesCachedResultWithoutNewAggregation() throws Exception {
        service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();
        clock.advance(Duration.ofMillis(1_000));

        ReportOutcome<GeneralReportSummary> outcome =
                service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();

        assertThat(outcome).isInstanceOf(ReportOutcome.Ready.class);
        assertThat(repository.aggregationCalls.get()).isEqualTo(1);
    }

    @Test
    void cacheMissDuringCooldownReturnsCooldownActiveWithoutNewAggregation() throws Exception {
        service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();
        clock.advance(Duration.ofMillis(1_000)); // within cooldown (5s), different window -> cache miss

        ReportOutcome<GeneralReportSummary> outcome =
                service.generalReport(user, PunishmentCategory.BAN, ReportWindow.WEEKLY).get();

        assertThat(outcome).isInstanceOf(ReportOutcome.CooldownActive.class);
        assertThat(repository.aggregationCalls.get()).isEqualTo(1);
    }

    @Test
    void freshAggregationRunsAgainAfterCacheAndCooldownExpire() throws Exception {
        service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();
        clock.advance(Duration.ofMillis(31_000)); // past both the 30s cache and the 5s cooldown

        ReportOutcome<GeneralReportSummary> outcome =
                service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();

        assertThat(outcome).isInstanceOf(ReportOutcome.Ready.class);
        assertThat(repository.aggregationCalls.get()).isEqualTo(2);
    }

    @Test
    void differentUsersEachGetTheirOwnCooldownButShareTheCache() throws Exception {
        service.generalReport(user, PunishmentCategory.BAN, ReportWindow.DAILY).get();

        UUID otherUser = UUID.randomUUID();
        ReportOutcome<GeneralReportSummary> outcome =
                service.generalReport(otherUser, PunishmentCategory.BAN, ReportWindow.DAILY).get();

        // Same key still within cache duration -> served from cache regardless of the second user's cooldown state.
        assertThat(outcome).isInstanceOf(ReportOutcome.Ready.class);
        assertThat(repository.aggregationCalls.get()).isEqualTo(1);
    }
}
