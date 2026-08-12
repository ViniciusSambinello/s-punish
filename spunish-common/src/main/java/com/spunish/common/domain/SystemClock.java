package com.spunish.common.domain;

import java.time.Instant;

/**
 * Port around {@link Instant#now()} so expiration and report-window
 * calculations can be driven by a fixed clock in tests.
 */
public interface SystemClock {

    Instant now();
}
