package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.duration.PunishmentDuration;

public record Reason(
        String id,
        PunishmentCategory category,
        String displayName,
        PunishmentDuration duration,
        String icon,
        String description,
        String permission) {

    public boolean isRestricted() {
        return permission != null && !permission.isBlank();
    }
}
