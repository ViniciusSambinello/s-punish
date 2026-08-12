package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentTarget;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class CommandSupport {

    private CommandSupport() {
    }

    public static void send(SPunishServices services, CommandSender sender, String key, Map<String, String> placeholders) {
        List<Component> lines = services.messageService().render(key, placeholders);
        for (Component line : lines) {
            sender.sendMessage(line);
        }
    }

    public static void resolveTarget(
            SPunishServices services, PlayerResolver resolver, CommandSender sender, String name,
            Consumer<PunishmentTarget> onFound) {
        resolver.resolve(name)
                .thenAccept(found -> {
                    if (found.isPresent()) {
                        onFound.accept(found.get());
                    } else {
                        services.mainThreadDispatcher().runOnMainThread(() ->
                                send(services, sender, "general.player-not-found", Map.of("player", name)));
                    }
                })
                .exceptionally(ex -> {
                    services.logger().log(Level.WARNING, "Player resolution failed for '" + name + "'", ex);
                    services.mainThreadDispatcher().runOnMainThread(() ->
                            send(services, sender, "general.internal-error", Map.of()));
                    return null;
                });
    }
}
