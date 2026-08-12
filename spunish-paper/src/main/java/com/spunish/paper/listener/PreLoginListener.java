package com.spunish.paper.listener;

import com.spunish.common.config.FailMode;
import com.spunish.common.domain.PlayerProfile;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.message.ComponentLines;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class PreLoginListener implements Listener {

    private final SPunishServices services;

    public PreLoginListener(SPunishServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        java.util.UUID uuid = event.getUniqueId();
        String name = event.getName();

        try {
            services.profileRepository().upsert(new PlayerProfile(uuid, name, services.clock().now())).join();
        } catch (RuntimeException failure) {
            if (denyOnFailure(event, "profile lookup", failure)) {
                return;
            }
        }

        Optional<Punishment> activeBan;
        try {
            activeBan = services.punishmentRepository().findActive(uuid, PunishmentCategory.BAN).join();
        } catch (RuntimeException failure) {
            denyOnFailure(event, "ban lookup", failure);
            return;
        }
        if (activeBan.isPresent()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, renderBanScreen(activeBan.get()));
            return;
        }

        try {
            Optional<Punishment> activeMute = services.punishmentRepository().findActive(uuid, PunishmentCategory.MUTE).join();
            services.stateCache().track(uuid, activeMute.orElse(null));
        } catch (RuntimeException failure) {
            services.logger().log(Level.WARNING, "Mute lookup failed during pre-login for " + name, failure);
            // Chat's fail mode governs this lookup: ALLOW seeds "not muted", DENY denies the
            // login outright.
            if (services.config().failMode().chat() == FailMode.DENY) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, storageUnavailableMessage());
                return;
            }
            services.stateCache().track(uuid, null);
        }
    }

    private boolean denyOnFailure(AsyncPlayerPreLoginEvent event, String what, RuntimeException failure) {
        services.logger().log(Level.WARNING, "Storage unavailable during " + what + " for " + event.getName(), failure);
        if (services.config().failMode().login() == FailMode.DENY) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, storageUnavailableMessage());
            return true;
        }
        return false;
    }

    private Component storageUnavailableMessage() {
        List<Component> lines = services.messageService().render("general.storage-unavailable", Map.of());
        return ComponentLines.join(lines);
    }

    private Component renderBanScreen(Punishment ban) {
        Map<String, String> placeholders = PunishmentPlaceholders.build(ban, services.messageService(), services.clock().now());
        List<Component> lines = services.messageService().render("screen.ban", placeholders);
        return ComponentLines.join(lines);
    }
}
