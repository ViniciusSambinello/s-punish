package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.duration.PunishmentDuration;

/**
 * A validated, ready-to-use catalog entry — the raw {@link ReasonDefinition}
 * with its duration text already parsed once, at load time.
 */
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
