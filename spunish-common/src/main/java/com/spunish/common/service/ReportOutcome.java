package com.spunish.common.service;

import java.time.Duration;

public sealed interface ReportOutcome<T> permits ReportOutcome.Ready, ReportOutcome.CooldownActive {

    record Ready<T>(T summary) implements ReportOutcome<T> {
    }

    record CooldownActive<T>(Duration remaining) implements ReportOutcome<T> {
    }
}
