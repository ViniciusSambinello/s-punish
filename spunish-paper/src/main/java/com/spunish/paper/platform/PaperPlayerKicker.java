package com.spunish.paper.platform;

import com.spunish.common.platform.PlayerKicker;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PaperPlayerKicker implements PlayerKicker {

    private final Server server;

    public PaperPlayerKicker(Server server) {
        this.server = server;
    }

    @Override
    public void kick(UUID uuid, Component reason) {
        Player player = server.getPlayer(uuid);
        if (player != null) {
            player.kick(reason);
        }
    }
}
