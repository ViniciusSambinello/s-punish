package com.spunish.paper.command;

import com.spunish.common.duration.PunishmentDuration;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.IssueCommand;
import com.spunish.common.service.IssueResult;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Turns an {@link IssueResult} into the right configurable message, and — on
 * success — triggers the same side effects the sync-consumption path uses
 * for a *local* apply: immediate local cache update, kick-if-online, and the
 * staff/public announcement. Shared by the command handler and, later, the
 * GUI click handler (section 9) so both apply routes behave identically.
 */
public final class IssueResultPresenter {

    private final SPunishServices services;

    public IssueResultPresenter(SPunishServices services) {
        this.services = services;
    }

    public void present(CommandSender sender, IssueCommand command, IssueResult result) {
        Instant now = services.clock().now();
        Map<String, String> base = new HashMap<>();
        base.put("target", command.target().name());
        base.put("category", command.category().name());
        base.put("reason-id", command.reason().id());
        base.put("reason-display", command.reason().displayName());

        switch (result) {
            case IssueResult.Success success -> {
                services.stateCache().onPunishmentCreated(success.applied());
                if (success.revokedByOverride() != null) {
                    services.stateCache().onPunishmentRevoked(success.revokedByOverride());
                }
                services.banEnforcer().kickIfOnline(success.applied());
                services.staffNotifier().announcePunishment(success.applied());
                sendRendered(sender, "punish.confirmation", PunishmentPlaceholders.build(success.applied(), services.messageService(), now));
            }
            case IssueResult.SelfPunishDenied ignored -> sendRendered(sender, "punish.self-punish-denied", base);
            case IssueResult.CategoryPermissionDenied ignored -> sendRendered(sender, "general.no-permission", base);
            case IssueResult.ReasonPermissionDenied ignored -> sendRendered(sender, "punish.reason-permission-denied", base);
            case IssueResult.TargetExempt ignored -> sendRendered(sender, "punish.target-exempt", base);
            case IssueResult.AlreadyActive already -> {
                Map<String, String> placeholders = new HashMap<>(base);
                placeholders.putAll(PunishmentPlaceholders.build(already.existing(), services.messageService(), now));
                sendRendered(sender, "punish.already-active", placeholders);
            }
            case IssueResult.DurationLimitExceeded exceeded -> {
                Map<String, String> placeholders = new HashMap<>(base);
                placeholders.put("duration-limit", services.messageService().formatDuration(
                        exceeded.limit() instanceof PunishmentDuration.Temporary temporary ? temporary.duration() : null));
                sendRendered(sender, "punish.duration-limit-exceeded", placeholders);
            }
            case IssueResult.InternalError error -> {
                services.logger().log(Level.WARNING, "Punishment could not be applied", error.cause());
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
