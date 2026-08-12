package com.spunish.paper.listener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared between {@link ChatListener} and {@link CommandBlockListener} (both
 * warn on the same cooldown) and cleared by {@link QuitListener} so it does
 * not grow unbounded over a long uptime. Public: constructed and threaded
 * through by {@code SPunishPlugin}, which lives in a different package.
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
