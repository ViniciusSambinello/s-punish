package com.spunish.common.domain;

import java.util.Objects;
import java.util.UUID;

public record PunishmentTarget(UUID uuid, String name) {

    public PunishmentTarget {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }
}
