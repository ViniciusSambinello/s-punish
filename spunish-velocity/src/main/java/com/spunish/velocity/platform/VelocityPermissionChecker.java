package com.spunish.velocity.platform;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.SystemActor;
import com.spunish.common.platform.PermissionChecker;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.UUID;

public final class VelocityPermissionChecker implements PermissionChecker {

    private final ProxyServer server;

    public VelocityPermissionChecker(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean hasPermission(Actor actor, String permission) {
        return switch (actor) {
            case PlayerActor player -> server.getPlayer(player.uuid()).map(p -> p.hasPermission(permission)).orElse(false);
            case ConsoleActor console -> true;
            case SystemActor system -> true;
        };
    }

    @Override
    public boolean targetHasPermission(UUID targetUuid, String permission) {
        return server.getPlayer(targetUuid).map((Player p) -> p.hasPermission(permission)).orElse(false);
    }
}
