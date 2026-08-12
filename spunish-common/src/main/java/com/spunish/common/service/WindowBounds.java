package com.spunish.common.service;

import java.time.Instant;

/**
 * {@code from == null} means "since the beginning" ({@link ReportWindow#ALL_TIME}).
 */
public record WindowBounds(Instant from, Instant to) {
}
