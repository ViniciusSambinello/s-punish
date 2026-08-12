package com.spunish.paper.platform;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.SystemActor;
import com.spunish.common.platform.PermissionChecker;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PaperPermissionChecker implements PermissionChecker {

    private final Server server;

    public PaperPermissionChecker(Server server) {
        this.server = server;
    }

    @Override
    public boolean hasPermission(Actor actor, String permission) {
        return switch (actor) {
            case PlayerActor player -> {
                Player online = server.getPlayer(player.uuid());
                yield online != null && online.hasPermission(permission);
            }
            case ConsoleActor console -> true;
            case SystemActor system -> true;
        };
    }

    @Override
    public boolean targetHasPermission(UUID targetUuid, String permission) {
        Player online = server.getPlayer(targetUuid);
        return online != null && online.hasPermission(permission);
    }
}
