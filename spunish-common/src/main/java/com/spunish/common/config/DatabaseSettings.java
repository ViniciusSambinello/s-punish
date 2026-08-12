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

    /**
     * Redacted on purpose: credentials must never reach a log line, including
     * through an uncaught exception that happens to embed this record.
     */
    @Override
    public String toString() {
        return "DatabaseSettings[host=" + host + ", port=" + port + ", database=" + database
                + ", user=" + user + ", password=REDACTED, tablePrefix=" + tablePrefix
                + ", useSsl=" + useSsl + ", pool=" + pool + ", queryTimeoutMs=" + queryTimeoutMs + "]";
    }
}
