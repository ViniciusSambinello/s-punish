package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;
import java.util.Map;

/**
 * {@code durationLimits} maps a permission suffix (the part after
 * {@code spunish.limit.}) to the maximum duration it grants — for example
 * {@code trial -> 7d} for {@code spunish.limit.trial}. A staffer matching no
 * entry has no duration restriction.
 */
@ConfigSerializable
public record SpunishConfig(
        DatabaseSettings database,
        ServerSettings server,
        SyncSettings sync,
        @Setting("fail-mode") FailModeSettings failMode,
        RetentionSettings retention,
        MuteSettings mute,
        ReportSettings report,
        @Setting("duration-limits") Map<String, String> durationLimits,
        @Setting("suggested-durations") List<String> suggestedDurations) {
}
