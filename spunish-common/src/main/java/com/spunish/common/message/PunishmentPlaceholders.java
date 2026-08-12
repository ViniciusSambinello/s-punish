package com.spunish.common.message;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.SystemActor;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The placeholder set every punishment/revocation-related message shares
 * (task 3.11's "common placeholders" list) — built once here so screens,
 * announcements, GUI lore and console output all resolve the same tokens
 * the same way.
 */
public final class PunishmentPlaceholders {

    private PunishmentPlaceholders() {
    }

    public static Map<String, String> build(Punishment punishment, MessageService messages, Instant now) {
        Map<String, String> map = new HashMap<>();
        map.put("target", punishment.targetName());
        map.put("actor", actorName(punishment.actor()));
        map.put("category", punishment.category().name());
        map.put("reason-id", punishment.reasonId());
        map.put("reason-display", punishment.reasonDisplay());
        map.put("applied-at", messages.formatInstant(punishment.createdAt()));
        map.put("punishment-id", punishment.publicId());
        map.put("origin-server", punishment.originServer());

        Duration originalDuration = punishment.isPermanent()
                ? null
                : Duration.between(punishment.createdAt(), punishment.expiresAt());
        map.put("duration", messages.formatDuration(originalDuration));

        Duration remaining = punishment.isPermanent() ? null : Duration.between(now, punishment.expiresAt());
        if (remaining != null && remaining.isNegative()) {
            remaining = Duration.ZERO;
        }
        map.put("duration-remaining", messages.formatDuration(remaining));

        if (punishment.isRevoked()) {
            map.put("revoker", actorName(punishment.revoker()));
            map.put("revoked-at", messages.formatInstant(punishment.revokedAt()));
            map.put("revoke-reason", punishment.revokeReason() == null ? "" : punishment.revokeReason());
        }
        return map;
    }

    private static String actorName(Actor actor) {
        return switch (actor) {
            case PlayerActor player -> player.name();
            case ConsoleActor console -> "Console";
            case SystemActor system -> "System";
        };
    }
}
