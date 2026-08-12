package com.spunish.paper.command;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Backs both {@code /unban} and {@code /unmute} — one instance per category,
 * each registered under its own label. Revoking a category never requires
 * permission to apply it (issuance and revocation are separate permissions).
 */
public final class UnpunishCommand implements BasicCommand {

    private final SPunishServices services;
    private final PlayerResolver playerResolver;
    private final PunishmentCategory category;
    private final String label;
    private final RevokeResultPresenter presenter;

    public UnpunishCommand(SPunishServices services, PlayerResolver playerResolver, PunishmentCategory category, String label) {
        this.services = services;
        this.playerResolver = playerResolver;
        this.category = category;
        this.label = label;
        this.presenter = new RevokeResultPresenter(services);
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0) {
            CommandSupport.send(services, sender, "unpunish.usage", Map.of("label", label));
            return;
        }

        Actor actor = Actors.of(sender);
        String playerName = args[0];
        String revokeReason = args.length > 1 ? String.join(" ", Arrays.asList(args).subList(1, args.length)) : null;

        CommandSupport.resolveTarget(services, playerResolver, sender, playerName, target ->
                services.revokeService().revoke(target.uuid(), category, actor, revokeReason).thenAccept(result ->
                        services.mainThreadDispatcher().runOnMainThread(() ->
                                presenter.present(sender, category, target.name(), result))));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return playerResolver.onlinePlayerNames(args[0]);
        }
        return List.of();
    }
}
