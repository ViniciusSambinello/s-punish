package com.spunish.paper.enforcement;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.message.ComponentLines;
import com.spunish.common.message.MessageService;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.platform.PlayerKicker;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * A BAN disconnects the target immediately if they are online on this
 * instance, whether the ban was applied locally or learned from a sync
 * event. No-op for MUTE and for a target not currently online here.
 */
public final class BanEnforcer {

    private final PlayerKicker playerKicker;
    private final MessageService messageService;
    private final SystemClock clock;

    public BanEnforcer(PlayerKicker playerKicker, MessageService messageService, SystemClock clock) {
        this.playerKicker = playerKicker;
        this.messageService = messageService;
        this.clock = clock;
    }

    public void kickIfOnline(Punishment ban) {
        if (ban.category() != PunishmentCategory.BAN) {
            return;
        }
        Map<String, String> placeholders = PunishmentPlaceholders.build(ban, messageService, clock.now());
        List<Component> lines = messageService.render("screen.ban", placeholders);
        playerKicker.kick(ban.targetUuid(), ComponentLines.join(lines));
    }
}
