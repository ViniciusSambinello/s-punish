package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record PoolSettings(
        @Setting("maximum-pool-size") int maximumPoolSize,
        @Setting("minimum-idle") int minimumIdle,
        @Setting("connection-timeout-ms") long connectionTimeoutMs,
        @Setting("max-lifetime-ms") long maxLifetimeMs) {
}
