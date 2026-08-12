package com.spunish.paper.command;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentState;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.HistoryEntry;
import com.spunish.common.service.HistoryPage;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * A player gets the history GUI; the console has no GUI, so it gets the same
 * data rendered as text (history spec's console fallback).
 */
public final class HistoryCommand implements BasicCommand {

    private static final int CONSOLE_PAGE_SIZE = 50;

    private final SPunishServices services;
    private final PlayerResolver playerResolver;
    private final MenuOpener menuOpener;

    public HistoryCommand(SPunishServices services, PlayerResolver playerResolver, MenuOpener menuOpener) {
        this.services = services;
        this.playerResolver = playerResolver;
        this.menuOpener = menuOpener;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        Actor actor = Actors.of(sender);
        if (!services.permissionChecker().hasPermission(actor, "spunish.history")) {
            CommandSupport.send(services, sender, "general.no-permission", Map.of());
            return;
        }
        if (args.length != 1) {
            CommandSupport.send(services, sender, "history.usage", Map.of());
            return;
        }

        CommandSupport.resolveTarget(services, playerResolver, sender, args[0], target -> {
            if (sender instanceof Player player) {
                services.mainThreadDispatcher().runOnMainThread(() -> menuOpener.openHistoryMenu(player, target));
            } else {
                showConsoleHistory(sender, target);
            }
        });
    }

    private void showConsoleHistory(CommandSender sender, PunishmentTarget target) {
        services.historyService().page(target.uuid(), Optional.empty(), 0, CONSOLE_PAGE_SIZE)
                .thenAccept(page -> services.mainThreadDispatcher().runOnMainThread(() -> renderConsoleHistory(sender, target, page)))
                .exceptionally(ex -> {
                    services.logger().log(Level.WARNING, "History lookup failed for " + target.name(), ex);
                    services.mainThreadDispatcher().runOnMainThread(() ->
                            CommandSupport.send(services, sender, "general.internal-error", Map.of()));
                    return null;
                });
    }

    private void renderConsoleHistory(CommandSender sender, PunishmentTarget target, HistoryPage page) {
        if (page.entries().isEmpty()) {
            CommandSupport.send(services, sender, "history.empty", Map.of("target", target.name()));
            return;
        }
        CommandSupport.send(services, sender, "history.console-header", Map.of("target", target.name()));
        Instant now = services.clock().now();
        for (HistoryEntry entry : page.entries()) {
            Map<String, String> placeholders = new HashMap<>(
                    PunishmentPlaceholders.build(entry.punishment(), services.messageService(), now));
            placeholders.put("state", stateLabel(entry.state()));
            CommandSupport.send(services, sender, "history.console-line", placeholders);
        }
    }

    private static String stateLabel(PunishmentState state) {
        return switch (state) {
            case PunishmentState.Active ignored -> "Active";
            case PunishmentState.Expired ignored -> "Expired";
            case PunishmentState.Revoked ignored -> "Revoked";
        };
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return playerResolver.onlinePlayerNames(args[0]);
        }
        return List.of();
    }
}
