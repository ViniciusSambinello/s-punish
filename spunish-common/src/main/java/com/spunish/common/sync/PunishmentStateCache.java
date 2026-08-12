package com.spunish.common.sync;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance mute state for players connected *to this instance* — see
 * design.md decision 9. Ban is deliberately never cached here: the
 * pre-login check reads storage directly, so no cached value can ever be
 * the reason a banned player gets in.
 *
 * {@code trackedPlayers} and {@code activeMuteByPlayer} are separate: the
 * absence of a mute entry must mean "not muted", not "not connected here" —
 * collapsing them into one map would make {@link #onPunishmentCreated} unable
 * to tell "this player isn't tracked" apart from "this player has no cached
 * mute yet" and silently drop the first mute a tracked, previously-unmuted
 * player receives.
 */
public final class PunishmentStateCache {

    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Punishment> activeMuteByPlayer = new ConcurrentHashMap<>();

    /**
     * Call on login, with whatever active mute (if any) was resolved from storage.
     */
    public void track(UUID playerUuid, Punishment activeMuteOrNull) {
        trackedPlayers.add(playerUuid);
        if (activeMuteOrNull != null) {
            activeMuteByPlayer.put(playerUuid, activeMuteOrNull);
        } else {
            activeMuteByPlayer.remove(playerUuid);
        }
    }

    /**
     * Call on quit.
     */
    public void discard(UUID playerUuid) {
        trackedPlayers.remove(playerUuid);
        activeMuteByPlayer.remove(playerUuid);
    }

    public Optional<Punishment> activeMute(UUID playerUuid) {
        return Optional.ofNullable(activeMuteByPlayer.get(playerUuid));
    }

    /**
     * Called both for a punishment applied locally (immediate effect, no
     * propagation delay) and for one learned from a sync event. A no-op for
     * bans and for players not tracked by this instance.
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
        // Only clear if this event is about the mute we actually have cached —
        // a stale/duplicate revoke event must not clear a newer mute.
        activeMuteByPlayer.computeIfPresent(punishment.targetUuid(),
                (uuid, current) -> current.id() == punishment.id() ? null : current);
    }
}
