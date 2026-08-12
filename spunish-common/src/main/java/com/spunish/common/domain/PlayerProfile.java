package com.spunish.common.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PlayerProfile(UUID uuid, String name, Instant lastSeenAt) {

    public PlayerProfile {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt");
    }
}
