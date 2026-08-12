package com.spunish.common.service;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.SystemActor;
import com.spunish.common.platform.PermissionChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Permissions are keyed per player UUID — granting to one actor must never leak to another. */
final class FakePermissionChecker implements PermissionChecker {

    private final Map<UUID, Set<String>> permissionsByPlayer = new HashMap<>();
    private final Set<String> targetPermissions = new HashSet<>();

    void grant(Actor actor, String permission) {
        if (actor instanceof PlayerActor player) {
            permissionsByPlayer.computeIfAbsent(player.uuid(), ignored -> new HashSet<>()).add(permission);
        }
    }

    void grantToTarget(String permission) {
        targetPermissions.add(permission);
    }

    @Override
    public boolean hasPermission(Actor actor, String permission) {
        if (actor instanceof ConsoleActor || actor instanceof SystemActor) {
            return true;
        }
        if (actor instanceof PlayerActor player) {
            return permissionsByPlayer.getOrDefault(player.uuid(), Set.of()).contains(permission);
        }
        return false;
    }

    @Override
    public boolean targetHasPermission(UUID targetUuid, String permission) {
        return targetPermissions.contains(permission);
    }
}
