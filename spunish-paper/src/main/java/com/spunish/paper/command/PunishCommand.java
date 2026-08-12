package com.spunish.paper.command;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.duration.DurationParser;
import com.spunish.common.duration.PunishmentDuration;
import com.spunish.common.service.IssueCommand;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /punish} has exactly three valid forms (issuance spec): a bare
 * player opens the category GUI, a player plus category opens the reason
 * GUI for it directly, and the full four-argument form applies the
 * punishment immediately with no GUI. Any other argument count is rejected.
 * The console has no GUI, so it may only use the full form.
 */
public final class PunishCommand implements BasicCommand {

    private final SPunishServices services;
    private final PlayerResolver playerResolver;
    private final MenuOpener menuOpener;
    private final IssueResultPresenter presenter;
    private final DurationParser durationParser = new DurationParser();

    public PunishCommand(SPunishServices services, PlayerResolver playerResolver, MenuOpener menuOpener) {
        this.services = services;
        this.playerResolver = playerResolver;
        this.menuOpener = menuOpener;
        this.presenter = new IssueResultPresenter(services);
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        Actor actor = Actors.of(sender);
        if (!services.permissionChecker().hasPermission(actor, "spunish.punish")) {
            send(sender, "general.no-permission", Map.of());
            return;
        }

        switch (args.length) {
            case 1 -> openCategoryMenu(sender, args[0]);
            case 2 -> openReasonMenu(sender, actor, args[0], args[1]);
            case 4 -> applyDirectly(sender, actor, args[0], args[1], args[2], args[3]);
            default -> send(sender, "punish.usage", Map.of());
        }
    }

    private void openCategoryMenu(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            send(sender, "punish.console-requires-full-form", Map.of());
            return;
        }
        CommandSupport.resolveTarget(services, playerResolver, sender, playerName, target ->
                services.mainThreadDispatcher().runOnMainThread(() -> menuOpener.openCategoryMenu(player, target)));
    }

    private void openReasonMenu(CommandSender sender, Actor actor, String playerName, String categoryText) {
        if (!(sender instanceof Player player)) {
            send(sender, "punish.console-requires-full-form", Map.of());
            return;
        }
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryText);
        if (category.isEmpty()) {
            send(sender, "general.invalid-category", Map.of());
            return;
        }
        if (!services.permissionChecker().hasPermission(actor, "spunish.punish." + category.get().permissionSuffix())) {
            send(sender, "general.no-permission", Map.of());
            return;
        }
        CommandSupport.resolveTarget(services, playerResolver, sender, playerName, target ->
                services.mainThreadDispatcher().runOnMainThread(() ->
                        menuOpener.openReasonMenu(player, target, category.get())));
    }

    private void applyDirectly(
            CommandSender sender, Actor actor, String playerName, String categoryText, String reasonId, String timeText) {
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryText);
        if (category.isEmpty()) {
            send(sender, "general.invalid-category", Map.of());
            return;
        }
        Optional<Reason> reason = services.catalogHolder().current().find(category.get(), reasonId);
        if (reason.isEmpty()) {
            send(sender, "punish.reason-not-found", Map.of("reason-id", reasonId, "category", category.get().name()));
            return;
        }
        Optional<PunishmentDuration> duration = durationParser.parse(timeText);
        if (duration.isEmpty()) {
            send(sender, "general.invalid-time", Map.of());
            return;
        }

        CommandSupport.resolveTarget(services, playerResolver, sender, playerName, target -> {
            IssueCommand command = new IssueCommand(target, actor, category.get(), reason.get(), duration.get());
            services.issueService().issue(command).thenAccept(result ->
                    services.mainThreadDispatcher().runOnMainThread(() -> presenter.present(sender, command, result)));
        });
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        Actor actor = Actors.of(stack.getSender());
        return switch (args.length) {
            case 1 -> playerResolver.onlinePlayerNames(args[0]);
            case 2 -> suggestCategories(actor, args[1]);
            case 3 -> suggestReasons(actor, args[1], args[2]);
            case 4 -> suggestDurations(args[1], args[2], args[3]);
            default -> List.of();
        };
    }

    private List<String> suggestCategories(Actor actor, String prefix) {
        List<String> allowed = new ArrayList<>();
        for (PunishmentCategory category : PunishmentCategory.values()) {
            if (services.permissionChecker().hasPermission(actor, "spunish.punish." + category.permissionSuffix())) {
                allowed.add(category.permissionSuffix());
            }
        }
        return filterByPrefix(allowed, prefix);
    }

    private List<String> suggestReasons(Actor actor, String categoryText, String prefix) {
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryText);
        if (category.isEmpty()) {
            return List.of();
        }
        List<Reason> visible = services.catalogHolder().current()
                .visibleTo(category.get(), permission -> services.permissionChecker().hasPermission(actor, permission));
        return filterByPrefix(visible.stream().map(Reason::id).toList(), prefix);
    }

    private List<String> suggestDurations(String categoryText, String reasonId, String prefix) {
        List<String> suggestions = new ArrayList<>();
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryText);
        category.flatMap(c -> services.catalogHolder().current().find(c, reasonId))
                .ifPresent(reason -> suggestions.add(reason.duration().canonicalText()));
        suggestions.addAll(services.config().suggestedDurations());
        return filterByPrefix(suggestions, prefix);
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)).toList();
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        CommandSupport.send(services, sender, key, placeholders);
    }
}
