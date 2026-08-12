package com.spunish.common.service;

import java.time.Duration;

/**
 * Shared by both {@link GeneralReportSummary} and {@link StafferReportSummary}
 * results — the cache/cooldown behavior in {@link ReportService} is identical
 * for both, so the outcome shape is too.
 */
public sealed interface ReportOutcome<T> permits ReportOutcome.Ready, ReportOutcome.CooldownActive {

    record Ready<T>(T summary) implements ReportOutcome<T> {
    }

    record CooldownActive<T>(Duration remaining) implements ReportOutcome<T> {
    }
}
