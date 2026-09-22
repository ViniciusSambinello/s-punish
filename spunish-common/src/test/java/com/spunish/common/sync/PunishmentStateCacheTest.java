package com.spunish.common.sync;

import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentStateCacheTest {

    private final UUID playerUuid = UUID.randomUUID();

    // Between each mute's createdAt (00:00) and expiresAt (01:00), so every
    // existing assertion below still observes the mute as active.
    private static final Instant NOW = Instant.parse("2026-01-01T00:30:00Z");

    private static Punishment mute(long id, UUID target) {
        return new Punishment(id, "PUB" + id, PunishmentCategory.MUTE, target, "Target",
                ConsoleActor.INSTANCE, "spam", "Spam", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"), "server-1", null, null, null);
    }

    private static Punishment ban(long id, UUID target) {
        return new Punishment(id, "PUB" + id, PunishmentCategory.BAN, target, "Target",
                ConsoleActor.INSTANCE, "hacking", "Hacking", Instant.parse("2026-01-01T00:00:00Z"),
                null, "server-1", null, null, null);
    }

    @Test
    void trackWithNoActiveMuteLeavesPlayerUnmuted() {
        PunishmentStateCache cache = new PunishmentStateCache();
        cache.track(playerUuid, null);

        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }

    @Test
    void trackWithActiveMuteIsQueryable() {
        PunishmentStateCache cache = new PunishmentStateCache();
        Punishment activeMute = mute(1, playerUuid);
        cache.track(playerUuid, activeMute);

        assertThat(cache.activeMute(playerUuid, NOW)).contains(activeMute);
    }

    @Test
    void discardClearsBothTrackingAndMuteState() {
        PunishmentStateCache cache = new PunishmentStateCache();
        cache.track(playerUuid, mute(1, playerUuid));

        cache.discard(playerUuid);

        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
        // And a stray create event after discard has no effect (not tracked anymore).
        cache.onPunishmentCreated(mute(2, playerUuid));
        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }

    @Test
    void onPunishmentCreatedUpdatesAPreviouslyUnmutedTrackedPlayer() {
        // A tracked player with no active mute must still pick up a newly created mute.
        PunishmentStateCache cache = new PunishmentStateCache();
        cache.track(playerUuid, null);

        Punishment newMute = mute(1, playerUuid);
        cache.onPunishmentCreated(newMute);

        assertThat(cache.activeMute(playerUuid, NOW)).contains(newMute);
    }

    @Test
    void onPunishmentCreatedIgnoresUntrackedPlayers() {
        PunishmentStateCache cache = new PunishmentStateCache();

        cache.onPunishmentCreated(mute(1, playerUuid));

        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }

    @Test
    void onPunishmentCreatedIgnoresBans() {
        PunishmentStateCache cache = new PunishmentStateCache();
        cache.track(playerUuid, null);

        cache.onPunishmentCreated(ban(1, playerUuid));

        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }

    @Test
    void onPunishmentRevokedClearsMatchingMute() {
        PunishmentStateCache cache = new PunishmentStateCache();
        Punishment activeMute = mute(1, playerUuid);
        cache.track(playerUuid, activeMute);

        cache.onPunishmentRevoked(activeMute);

        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }

    @Test
    void staleRevokeEventDoesNotClearANewerMute() {
        PunishmentStateCache cache = new PunishmentStateCache();
        cache.track(playerUuid, mute(1, playerUuid));
        Punishment newerMute = mute(2, playerUuid);
        cache.onPunishmentCreated(newerMute);

        // A late-arriving revoke event for the OLD mute must not clear the new one.
        cache.onPunishmentRevoked(mute(1, playerUuid));

        assertThat(cache.activeMute(playerUuid, NOW)).contains(newerMute);
    }

    @Test
    void activeMuteStopsBeingReportedOnceItsOwnExpiryHasPassed() {
        // A tracked mute is never proactively swept out of the cache — it must
        // stop being enforced as soon as `now` is past its own expiresAt,
        // even though the entry is still physically present.
        PunishmentStateCache cache = new PunishmentStateCache();
        Punishment expiring = mute(1, playerUuid);
        cache.track(playerUuid, expiring);

        Instant afterExpiry = expiring.expiresAt().plusSeconds(1);
        assertThat(cache.activeMute(playerUuid, afterExpiry)).isEmpty();

        // And the now-stale entry must not resurface for an earlier instant either.
        assertThat(cache.activeMute(playerUuid, NOW)).isEmpty();
    }
}
