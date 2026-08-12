package com.spunish.common.platform;

import com.spunish.common.domain.Actor;

import java.util.UUID;

/**
 * Pure permission-node checks (see design.md decision 12) — implemented per
 * platform against {@code Permissible} (Paper) or {@code CommandSource}
 * (Velocity), with no dependency on any specific permissions plugin.
 * Implementations are expected to grant every permission to {@code ConsoleActor}
 * and {@code SystemActor}, matching vanilla console behavior.
 */
public interface PermissionChecker {

    boolean hasPermission(Actor actor, String permission);

    /**
     * Checks a permission against the *target* of a punishment, who may be
     * offline. Vanilla Bukkit/Velocity permission checks only work for online
     * sessions (checking an {@code OfflinePlayer} isn't possible without
     * depending on a specific permissions plugin, which design.md rules out) —
     * implementations return {@code false} when the target is offline, which
     * makes an unverifiable exemption claim resolve to "not exempt" rather
     * than silently blocking every offline punishment.
     */
    boolean targetHasPermission(UUID targetUuid, String permission);
}
