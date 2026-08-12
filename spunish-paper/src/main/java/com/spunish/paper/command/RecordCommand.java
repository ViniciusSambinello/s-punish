package com.spunish.paper.command;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.paper.SPunishServices;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /record <category>} opens the staff-wide report; {@code /record
 * <category> <staffer>} restricts it to one member. Viewing anyone else's
 * report requires {@code spunish.record.others} in addition to
 * {@code spunish.record} — a staffer may always see their own.
 */
public final class RecordCommand implements BasicCommand {

    private final SPunishServices services;
    private final PlayerResolver playerResolver;
    private final MenuOpener menuOpener;

    public RecordCommand(SPunishServices services, PlayerResolver playerResolver, MenuOpener menuOpener) {
        this.services = services;
        this.playerResolver = playerResolver;
        this.menuOpener = menuOpener;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        Actor actor = Actors.of(sender);
        if (!services.permissionChecker().hasPermission(actor, "spunish.record")) {
            CommandSupport.send(services, sender, "general.no-permission", Map.of());
            return;
        }
        if (args.length != 1 && args.length != 2) {
            CommandSupport.send(services, sender, "record.usage", Map.of());
            return;
        }
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(args[0]);
        if (category.isEmpty()) {
            CommandSupport.send(services, sender, "general.invalid-category", Map.of());
            return;
        }
        if (!(sender instanceof Player player)) {
            CommandSupport.send(services, sender, "general.console-not-supported", Map.of());
            return;
        }

        if (args.length == 1) {
            if (!services.permissionChecker().hasPermission(actor, "spunish.record.others")) {
                CommandSupport.send(services, sender, "general.no-permission", Map.of());
                return;
            }
            services.mainThreadDispatcher().runOnMainThread(() -> menuOpener.openGeneralReportMenu(player, category.get()));
            return;
        }

        CommandSupport.resolveTarget(services, playerResolver, sender, args[1], staffer -> {
            boolean isSelf = staffer.uuid().equals(player.getUniqueId());
            if (!isSelf && !services.permissionChecker().hasPermission(actor, "spunish.record.others")) {
                services.mainThreadDispatcher().runOnMainThread(() ->
                        CommandSupport.send(services, sender, "general.no-permission", Map.of()));
                return;
            }
            services.mainThreadDispatcher().runOnMainThread(() ->
                    menuOpener.openStafferReportMenu(player, category.get(), staffer));
        });
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return switch (args.length) {
            case 1 -> filterByPrefix(List.of("ban", "mute"), args[0]);
            case 2 -> suggestStaffers(args[0], args[1]);
            default -> List.of();
        };
    }

    private List<String> suggestStaffers(String categoryText, String prefix) {
        Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryText);
        if (category.isEmpty()) {
            return List.of();
        }
        String permission = "spunish.punish." + category.get().permissionSuffix();
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                names.add(online.getName());
            }
        }
        return filterByPrefix(names, prefix);
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)).toList();
    }
}
