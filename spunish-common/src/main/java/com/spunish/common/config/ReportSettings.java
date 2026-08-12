package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record ReportSettings(
        @Setting("cooldown-ms") long cooldownMs,
        @Setting("cache-duration-ms") long cacheDurationMs,
        @Setting("ranking-limit") int rankingLimit) {
}
