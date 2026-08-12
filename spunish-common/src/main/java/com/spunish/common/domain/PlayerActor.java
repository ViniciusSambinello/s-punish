package com.spunish.common.domain;

import java.util.Objects;
import java.util.UUID;

public record PlayerActor(UUID uuid, String name) implements Actor {

    public PlayerActor {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }
}
