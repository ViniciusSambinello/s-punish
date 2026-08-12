package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record DatabaseSettings(
        String host,
        int port,
        String database,
        String user,
        String password,
        @Setting("table-prefix") String tablePrefix,
        @Setting("use-ssl") boolean useSsl,
        PoolSettings pool,
        @Setting("query-timeout-ms") long queryTimeoutMs) {
}
