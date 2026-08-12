package com.spunish.velocity;

import com.spunish.common.config.FailMode;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.message.ComponentLines;
import com.spunish.common.message.PunishmentPlaceholders;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;

import java.util.Map;
import java.util.logging.Level;

/**
 * Refuses a banned player at login, before they are forwarded to any
 * backend, with the same configurable ban screen a backend shows. On a
 * storage failure, the configured login fail mode applies.
 */
final class BanLoginListener {

    private final SPunishVelocityServices services;

    BanLoginListener(SPunishVelocityServices services) {
        this.services = services;
    }

    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        return EventTask.resumeWhenComplete(
                services.punishmentRepository().findActive(event.getPlayer().getUniqueId(), PunishmentCategory.BAN)
                        .thenAccept(activeBan -> activeBan.ifPresent(ban -> deny(event, "screen.ban",
                                PunishmentPlaceholders.build(ban, services.messageService(), services.clock().now()))))
                        .exceptionally(ex -> {
                            services.logger().log(Level.WARNING,
                                    "Ban lookup failed during login for " + event.getPlayer().getUsername(), ex);
                            if (services.config().failMode().login() == FailMode.DENY) {
                                deny(event, "general.storage-unavailable", Map.of());
                            }
                            return null;
                        }));
    }

    private void deny(LoginEvent event, String messageKey, Map<String, String> placeholders) {
        var lines = services.messageService().render(messageKey, placeholders);
        event.setResult(ResultedEvent.ComponentResult.denied(ComponentLines.join(lines)));
    }
}
