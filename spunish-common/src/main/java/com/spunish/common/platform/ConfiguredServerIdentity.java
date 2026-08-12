package com.spunish.common.platform;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Falls back to the local hostname (stable across restarts on the same
 * machine, though not in every containerized setup) when {@code server.id}
 * is blank, and warns once so the operator sets it explicitly — see
 * {@code punishment/network-sync}'s server identity requirement.
 */
public final class ConfiguredServerIdentity implements ServerIdentity {

    private final String id;

    public ConfiguredServerIdentity(String configuredId, Logger logger) {
        if (configuredId != null && !configuredId.isBlank()) {
            this.id = configuredId;
        } else {
            this.id = deriveFallbackId();
            logger.warning("server.id is not configured; derived '" + id
                    + "'. Set it explicitly in config.yml for a stable identifier across restarts.");
        }
    }

    @Override
    public String id() {
        return id;
    }

    private static String deriveFallbackId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "server-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
