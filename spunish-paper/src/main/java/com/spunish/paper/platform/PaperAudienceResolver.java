package com.spunish.paper.platform;

import com.spunish.common.platform.AudienceResolver;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class PaperAudienceResolver implements AudienceResolver {

    private final Server server;

    public PaperAudienceResolver(Server server) {
        this.server = server;
    }

    @Override
    public Optional<Audience> online(UUID uuid) {
        Player player = server.getPlayer(uuid);
        return Optional.ofNullable(player);
    }

    @Override
    public Collection<Audience> onlineWithPermission(String permission) {
        return server.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .map(Audience.class::cast)
                .toList();
    }

    @Override
    public Audience broadcastAudience() {
        return server;
    }
}
