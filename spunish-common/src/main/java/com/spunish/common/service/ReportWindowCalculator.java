package com.spunish.common.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Daily is calendar/zone-aware (midnight in the configured zone, not UTC);
 * weekly and monthly are rolling windows of complete days ending now, per
 * the exact definitions in {@code punishment/reporting}.
 */
public final class ReportWindowCalculator {

    public WindowBounds bounds(ReportWindow window, Instant now, ZoneId zone) {
        return switch (window) {
            case DAILY -> new WindowBounds(now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant(), now);
            case WEEKLY -> new WindowBounds(now.minus(Duration.ofDays(7)), now);
            case MONTHLY -> new WindowBounds(now.minus(Duration.ofDays(30)), now);
            case ALL_TIME -> new WindowBounds(null, now);
        };
    }
}
