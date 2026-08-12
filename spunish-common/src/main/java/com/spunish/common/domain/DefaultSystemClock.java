package com.spunish.common.domain;

import java.time.Instant;

public final class DefaultSystemClock implements SystemClock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
