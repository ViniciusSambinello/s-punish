package com.spunish.common.sync;

import com.spunish.common.domain.Punishment;

/**
 * Reacts to a sync event already resolved to its full {@link Punishment} —
 * the raw event only carries an id/type/target, not enough to decide how to
 * react (a mute needs unblocking chat, a ban needs a kick). Implementations
 * run on whatever thread {@link SyncEventConsumer} calls them from and are
 * responsible for hopping to the main thread themselves if the platform needs that.
 */
public interface SyncEventListener {

    void onPunishmentCreated(Punishment punishment);

    void onPunishmentRevoked(Punishment punishment);
}
