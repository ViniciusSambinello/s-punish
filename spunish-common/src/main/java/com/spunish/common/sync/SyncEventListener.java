package com.spunish.common.sync;

import com.spunish.common.domain.Punishment;

/**
 * Reacts to a sync event resolved to its {@link Punishment}: a mute needs
 * unblocking chat, a ban needs a kick.
 */
public interface SyncEventListener {

    void onPunishmentCreated(Punishment punishment);

    void onPunishmentRevoked(Punishment punishment);
}
