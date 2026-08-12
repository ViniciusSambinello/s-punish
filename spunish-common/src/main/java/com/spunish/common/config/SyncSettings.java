package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record SyncSettings(
        @Setting("poll-interval-ms") long pollIntervalMs,
        @Setting("overlap-ms") long overlapMs,
        @Setting("event-retention-ms") long eventRetentionMs) {
}
