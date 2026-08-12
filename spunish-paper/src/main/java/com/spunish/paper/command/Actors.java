package com.spunish.paper.command;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Actors {

    private Actors() {
    }

    public static Actor of(CommandSender sender) {
        if (sender instanceof Player player) {
            return new PlayerActor(player.getUniqueId(), player.getName());
        }
        return ConsoleActor.INSTANCE;
    }
}
