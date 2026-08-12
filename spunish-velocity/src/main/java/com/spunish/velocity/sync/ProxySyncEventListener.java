package com.spunish.velocity.sync;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.message.ComponentLines;
import com.spunish.common.message.MessageService;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.platform.PlayerKicker;
import com.spunish.common.sync.SyncEventListener;

/**
 * The proxy only enforces bans — a mute has nothing for it to do, and lifting
 * a ban never needs to disconnect anyone.
 */
public final class ProxySyncEventListener implements SyncEventListener {

    private final PlayerKicker playerKicker;
    private final MessageService messageService;
    private final SystemClock clock;

    public ProxySyncEventListener(PlayerKicker playerKicker, MessageService messageService, SystemClock clock) {
        this.playerKicker = playerKicker;
        this.messageService = messageService;
        this.clock = clock;
    }

    @Override
    public void onPunishmentCreated(Punishment punishment) {
        if (punishment.category() != PunishmentCategory.BAN) {
            return;
        }
        var placeholders = PunishmentPlaceholders.build(punishment, messageService, clock.now());
        playerKicker.kick(punishment.targetUuid(), ComponentLines.join(messageService.render("screen.ban", placeholders)));
    }

    @Override
    public void onPunishmentRevoked(Punishment punishment) {
    }
}
