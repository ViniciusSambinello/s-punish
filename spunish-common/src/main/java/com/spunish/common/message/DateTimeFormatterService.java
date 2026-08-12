package com.spunish.common.message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Punishment times are persisted in UTC; staff and players always see them
 * rendered in the configured local time zone.
 */
public final class DateTimeFormatterService {

    private final DateTimeFormatter formatter;

    public DateTimeFormatterService(String pattern, ZoneId zone) {
        this.formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone);
    }

    public String format(Instant instant) {
        return formatter.format(instant);
    }

    public ZoneId zone() {
        return formatter.getZone();
    }
}
