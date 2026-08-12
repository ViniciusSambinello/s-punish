package com.spunish.velocity.platform;

import com.spunish.common.platform.PlayerKicker;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public final class VelocityPlayerKicker implements PlayerKicker {

    private final ProxyServer server;

    public VelocityPlayerKicker(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void kick(UUID uuid, Component reason) {
        server.getPlayer(uuid).ifPresent(player -> player.disconnect(reason));
    }
}
