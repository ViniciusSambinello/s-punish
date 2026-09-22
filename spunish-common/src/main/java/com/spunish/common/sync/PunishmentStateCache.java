package com.spunish.common.sync;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance mute state for players connected to this instance
 * (design.md decision 9). Ban is deliberately never cached here: the
 * pre-login check always reads storage directly, so a cached value can
 * never be the reason a banned player gets in.
 */
public final class PunishmentStateCache {

    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Punishment> activeMuteByPlayer = new ConcurrentHashMap<>();

    public void track(UUID playerUuid, Punishment activeMuteOrNull) {
        trackedPlayers.add(playerUuid);
        if (activeMuteOrNull != null) {
            activeMuteByPlayer.put(playerUuid, activeMuteOrNull);
        } else {
            activeMuteByPlayer.remove(playerUuid);
        }
    }

    public void discard(UUID playerUuid) {
        trackedPlayers.remove(playerUuid);
        activeMuteByPlayer.remove(playerUuid);
    }

    /**
     * A cached mute is only ever cleared by an explicit revoke (sync event or
     * local revoke) or by a fresh {@link #track}, never by a background
     * sweep — so a timed mute a player outlasts while staying connected must
     * be checked against {@code now} here, at read time, or enforcement
     * would keep blocking chat/commands for a mute that has already expired.
     */
    public Optional<Punishment> activeMute(UUID playerUuid, Instant now) {
        Punishment mute = activeMuteByPlayer.get(playerUuid);
        if (mute == null) {
            return Optional.empty();
        }
        if (!mute.isPermanent() && !mute.expiresAt().isAfter(now)) {
            activeMuteByPlayer.remove(playerUuid, mute);
            return Optional.empty();
        }
        return Optional.of(mute);
    }

    /**
     * A no-op for bans (ban state is never cached) and for players not
     * tracked by this instance.
     */
    public void onPunishmentCreated(Punishment punishment) {
        if (punishment.category() != PunishmentCategory.MUTE) {
            return;
        }
        if (trackedPlayers.contains(punishment.targetUuid())) {
            activeMuteByPlayer.put(punishment.targetUuid(), punishment);
        }
    }

    public void onPunishmentRevoked(Punishment punishment) {
        if (punishment.category() != PunishmentCategory.MUTE) {
            return;
        }
        // A stale or duplicate revoke event must not clear a newer mute.
        activeMuteByPlayer.computeIfPresent(punishment.targetUuid(),
                (uuid, current) -> current.id() == punishment.id() ? null : current);
    }
}
