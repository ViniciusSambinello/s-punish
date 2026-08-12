package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.RevokeResult;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RevokeResultPresenter {

    private final SPunishServices services;

    public RevokeResultPresenter(SPunishServices services) {
        this.services = services;
    }

    public void present(CommandSender sender, PunishmentCategory category, String targetName, RevokeResult result) {
        Map<String, String> base = Map.of("target", targetName, "category", category.name());

        switch (result) {
            case RevokeResult.Success success -> {
                services.stateCache().onPunishmentRevoked(success.revoked());
                services.staffNotifier().announceRevocation(success.revoked());
                sendRendered(sender, "unpunish.confirmation",
                        PunishmentPlaceholders.build(success.revoked(), services.messageService(), services.clock().now()));
            }
            case RevokeResult.NoActivePunishment ignored -> sendRendered(sender, "unpunish.no-active-punishment", base);
            case RevokeResult.PermissionDenied ignored -> sendRendered(sender, "general.no-permission", base);
            case RevokeResult.InternalError error -> {
                services.logger().log(Level.WARNING, "Revocation failed", error.cause());
                sendRendered(sender, "general.internal-error", base);
            }
        }
    }

    private void sendRendered(CommandSender sender, String key, Map<String, String> placeholders) {
        List<Component> lines = services.messageService().render(key, placeholders);
        for (Component line : lines) {
            sender.sendMessage(line);
        }
    }
}
