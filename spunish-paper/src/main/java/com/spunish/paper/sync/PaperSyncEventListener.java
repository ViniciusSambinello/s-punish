package com.spunish.paper.sync;

import com.spunish.common.domain.Punishment;
import com.spunish.common.platform.MainThreadDispatcher;
import com.spunish.common.sync.PunishmentStateCache;
import com.spunish.common.sync.SyncEventListener;
import com.spunish.paper.enforcement.BanEnforcer;
import com.spunish.paper.enforcement.StaffNotifier;

/**
 * Reacts to a sync event that this instance did not itself originate — by
 * construction (see {@code SyncEventConsumer}), every call here is about a
 * change made elsewhere in the network. Dispatched to the main thread
 * conservatively: kicking a player and sending Adventure messages are
 * documented safe off-thread in places, but not uniformly enough to rely on.
 */
public final class PaperSyncEventListener implements SyncEventListener {

    private final PunishmentStateCache stateCache;
    private final BanEnforcer banEnforcer;
    private final StaffNotifier staffNotifier;
    private final MainThreadDispatcher mainThreadDispatcher;

    public PaperSyncEventListener(
            PunishmentStateCache stateCache,
            BanEnforcer banEnforcer,
            StaffNotifier staffNotifier,
            MainThreadDispatcher mainThreadDispatcher) {
        this.stateCache = stateCache;
        this.banEnforcer = banEnforcer;
        this.staffNotifier = staffNotifier;
        this.mainThreadDispatcher = mainThreadDispatcher;
    }

    @Override
    public void onPunishmentCreated(Punishment punishment) {
        mainThreadDispatcher.runOnMainThread(() -> {
            stateCache.onPunishmentCreated(punishment);
            banEnforcer.kickIfOnline(punishment);
            staffNotifier.announcePunishment(punishment);
        });
    }

    @Override
    public void onPunishmentRevoked(Punishment punishment) {
        mainThreadDispatcher.runOnMainThread(() -> {
            stateCache.onPunishmentRevoked(punishment);
            staffNotifier.announceRevocation(punishment);
        });
    }
}
