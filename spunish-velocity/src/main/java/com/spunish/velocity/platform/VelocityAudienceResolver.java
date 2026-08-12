package com.spunish.velocity.platform;

import com.spunish.common.platform.AudienceResolver;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.audience.Audience;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class VelocityAudienceResolver implements AudienceResolver {

    private final ProxyServer server;

    public VelocityAudienceResolver(ProxyServer server) {
        this.server = server;
    }

    @Override
    public Optional<Audience> online(UUID uuid) {
        return server.getPlayer(uuid).map(Audience.class::cast);
    }

    @Override
    public Collection<Audience> onlineWithPermission(String permission) {
        return server.getAllPlayers().stream()
                .filter((Player player) -> player.hasPermission(permission))
                .map(Audience.class::cast)
                .toList();
    }

    @Override
    public Audience broadcastAudience() {
        return server;
    }
}
