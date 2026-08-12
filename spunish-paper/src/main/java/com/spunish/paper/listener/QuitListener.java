package com.spunish.paper.listener;

import com.spunish.paper.SPunishServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QuitListener implements Listener {

    private final SPunishServices services;
    private final MuteWarningCooldown cooldown;

    public QuitListener(SPunishServices services, MuteWarningCooldown cooldown) {
        this.services = services;
        this.cooldown = cooldown;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        services.stateCache().discard(event.getPlayer().getUniqueId());
        cooldown.forget(event.getPlayer().getUniqueId());
    }
}
