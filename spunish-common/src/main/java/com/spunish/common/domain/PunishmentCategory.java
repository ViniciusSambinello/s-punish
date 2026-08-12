package com.spunish.common.domain;

import java.util.Locale;
import java.util.Optional;

public enum PunishmentCategory {
    BAN,
    MUTE;

    public static Optional<PunishmentCategory> fromString(String text) {
        if (text == null) {
            return Optional.empty();
        }
        for (PunishmentCategory category : values()) {
            if (category.name().equalsIgnoreCase(text)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    /**
     * Lowercase form used to build permission nodes such as {@code spunish.punish.ban}.
     */
    public String permissionSuffix() {
        return name().toLowerCase(Locale.ROOT);
    }
}
