package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record RetentionSettings(
        boolean enabled,
        @Setting("retention-days") int retentionDays) {
}
