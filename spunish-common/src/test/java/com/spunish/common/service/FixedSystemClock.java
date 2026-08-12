package com.spunish.common.service;

import com.spunish.common.domain.SystemClock;

import java.time.Duration;
import java.time.Instant;

final class FixedSystemClock implements SystemClock {

    private Instant instant;

    FixedSystemClock(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Instant now() {
        return instant;
    }

    void advance(Duration amount) {
        instant = instant.plus(amount);
    }
}
