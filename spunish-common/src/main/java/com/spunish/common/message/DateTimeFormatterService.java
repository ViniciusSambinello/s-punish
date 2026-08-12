package com.spunish.common.message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders a UTC {@link Instant} using the configurable pattern and time zone
 * from {@code messages.yml} — everything is persisted in UTC, but staff and
 * players should see local time.
 */
public final class DateTimeFormatterService {

    private final DateTimeFormatter formatter;

    public DateTimeFormatterService(String pattern, ZoneId zone) {
        this.formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone);
    }

    public String format(Instant instant) {
        return formatter.format(instant);
    }
}
