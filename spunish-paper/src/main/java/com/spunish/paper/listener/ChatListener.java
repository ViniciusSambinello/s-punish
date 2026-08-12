package com.spunish.paper.listener;

import com.spunish.common.domain.Punishment;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chat mute checks read only from the punishment state cache, never storage
 * directly. A muted message is dropped entirely — never delivered and never
 * logged to chat.
 */
public final class ChatListener implements Listener {

    private final SPunishServices services;
    private final MuteWarningCooldown cooldown;

    public ChatListener(SPunishServices services, MuteWarningCooldown cooldown) {
        this.services = services;
        this.cooldown = cooldown;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = services.stateCache().activeMute(player.getUniqueId());
        if (activeMute.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        warnIfNotOnCooldown(player, activeMute.get());
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
