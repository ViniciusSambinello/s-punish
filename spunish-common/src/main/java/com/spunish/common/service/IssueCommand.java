package com.spunish.common.service;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.duration.PunishmentDuration;

/**
 * @param explicitDuration non-null only for the full command form
 *                          ({@code /punish <player> <category> <reason> <time>}), which overrides
 *                          the reason's own configured duration. {@code null} means "use
 *                          {@code reason.duration()}" — the GUI/click path, which has no time argument.
 */
public record IssueCommand(
        PunishmentTarget target,
        Actor actor,
        PunishmentCategory category,
        Reason reason,
        PunishmentDuration explicitDuration) {

    public PunishmentDuration effectiveDuration() {
        return explicitDuration != null ? explicitDuration : reason.duration();
    }
}
