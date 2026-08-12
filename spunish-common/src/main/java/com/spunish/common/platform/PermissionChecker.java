package com.spunish.common.platform;

import com.spunish.common.domain.Actor;

import java.util.UUID;

/**
 * Implementations grant every permission to {@code ConsoleActor} and
 * {@code SystemActor}, matching vanilla console behavior.
 */
public interface PermissionChecker {

    boolean hasPermission(Actor actor, String permission);

    /**
     * Checks a permission against the *target* of a punishment, who may be
     * offline. When the target is offline this returns {@code false}, so an
     * unverifiable exemption claim resolves to "not exempt" rather than
     * blocking every offline punishment.
     */
    boolean targetHasPermission(UUID targetUuid, String permission);
}
