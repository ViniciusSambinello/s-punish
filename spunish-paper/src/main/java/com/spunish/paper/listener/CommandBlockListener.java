package com.spunish.paper.listener;

import com.spunish.common.domain.Punishment;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Blocks the configured communication commands (task 7.7) for a muted
 * player. A command not registered by a plugin (a vanilla one) has no
 * {@link PluginCommand} to resolve, so vanilla aliases must already be
 * listed explicitly in {@code mute.blocked-commands} (the shipped default
 * does this — msg/tell/w/r/reply/mail); a plugin-registered alias, which
 * *is* resolvable, is caught even if only its primary name is configured.
 */
public final class CommandBlockListener implements Listener {

    private final SPunishServices services;
    private final Server server;
    private final MuteWarningCooldown cooldown;

    public CommandBlockListener(SPunishServices services, Server server, MuteWarningCooldown cooldown) {
        this.services = services;
        this.server = server;
        this.cooldown = cooldown;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = services.stateCache().activeMute(player.getUniqueId());
        if (activeMute.isEmpty()) {
            return;
        }

        String label = extractLabel(event.getMessage());
        Set<String> blocked = lowercaseBlockedCommands();
        if (!isBlocked(label, blocked)) {
            return;
        }

        event.setCancelled(true);
        warnIfNotOnCooldown(player, activeMute.get());
    }

    private Set<String> lowercaseBlockedCommands() {
        return services.config().mute().blockedCommands().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean isBlocked(String label, Set<String> blocked) {
        if (blocked.contains(label.toLowerCase(Locale.ROOT))) {
            return true;
        }
        PluginCommand pluginCommand = server.getPluginCommand(label);
        if (pluginCommand == null) {
            return false;
        }
        if (blocked.contains(pluginCommand.getName().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return pluginCommand.getAliases().stream()
                .anyMatch(alias -> blocked.contains(alias.toLowerCase(Locale.ROOT)));
    }

    private static String extractLabel(String message) {
        String withoutSlash = message.startsWith("/") ? message.substring(1) : message;
        int spaceIndex = withoutSlash.indexOf(' ');
        return spaceIndex == -1 ? withoutSlash : withoutSlash.substring(0, spaceIndex);
    }

    private void warnIfNotOnCooldown(Player player, Punishment mute) {
        Instant now = services.clock().now();
        long cooldownMs = services.config().mute().warningCooldownMs();
        if (cooldown.isOnCooldown(player.getUniqueId(), now, cooldownMs)) {
            return;
        }
        cooldown.recordWarning(player.getUniqueId(), now);

        Map<String, String> placeholders = PunishmentPlaceholders.build(mute, services.messageService(), now);
        List<Component> lines = services.messageService().render("screen.mute-warning", placeholders);
        for (Component line : lines) {
            player.sendMessage(line);
        }
    }
}
