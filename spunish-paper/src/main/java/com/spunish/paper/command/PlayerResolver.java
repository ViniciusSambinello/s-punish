package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.storage.ProfileRepository;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves a name to a target that may be online or a previously-known
 * offline player (issuance spec's "target player resolution") — online is
 * checked first (no storage round-trip needed), offline falls back to the
 * profiles table.
 */
public final class PlayerResolver {

    private final Server server;
    private final ProfileRepository profileRepository;

    public PlayerResolver(Server server, ProfileRepository profileRepository) {
        this.server = server;
        this.profileRepository = profileRepository;
    }

    public CompletableFuture<Optional<PunishmentTarget>> resolve(String name) {
        Player online = server.getPlayerExact(name);
        if (online != null) {
            return CompletableFuture.completedFuture(Optional.of(new PunishmentTarget(online.getUniqueId(), online.getName())));
        }
        return profileRepository.findByName(name)
                .thenApply(profile -> profile.map(p -> new PunishmentTarget(p.uuid(), p.name())));
    }

    public List<String> onlinePlayerNames(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return server.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }
}
