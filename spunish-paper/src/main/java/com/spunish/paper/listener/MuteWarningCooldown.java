package com.spunish.paper.listener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared between chat and command blocking so a muted player is not warned
 * twice within the same cooldown window.
 */
public final class MuteWarningCooldown {

    private final Map<UUID, Instant> lastWarnedAt = new ConcurrentHashMap<>();

    boolean isOnCooldown(UUID uuid, Instant now, long cooldownMs) {
        Instant last = lastWarnedAt.get(uuid);
        return last != null && now.isBefore(last.plusMillis(cooldownMs));
    }

    void recordWarning(UUID uuid, Instant now) {
        lastWarnedAt.put(uuid, now);
    }

    void forget(UUID uuid) {
        lastWarnedAt.remove(uuid);
    }
}
