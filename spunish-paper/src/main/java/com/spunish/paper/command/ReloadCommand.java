package com.spunish.paper.command;

import com.spunish.common.domain.Actor;
import com.spunish.paper.ReloadOutcome;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reloads only {@code reasons.yml} and {@code messages.yml} — the admin
 * reload command's scope per the catalog and messaging specs.
 */
public final class ReloadCommand implements BasicCommand {

    private final SPunishServices services;

    public ReloadCommand(SPunishServices services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        Actor actor = Actors.of(sender);
        if (!services.permissionChecker().hasPermission(actor, "spunish.admin.reload")) {
            CommandSupport.send(services, sender, "general.no-permission", Map.of());
            return;
        }

        ReloadOutcome outcome = services.reloadCatalogAndMessages();
        if (outcome.ok()) {
            CommandSupport.send(services, sender, "reload.success", Map.of());
            return;
        }
        CommandSupport.send(services, sender, "reload.failure-header", Map.of());
        for (String error : outcome.errors()) {
            CommandSupport.send(services, sender, "reload.failure-line", Map.of("error", error));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }
}
