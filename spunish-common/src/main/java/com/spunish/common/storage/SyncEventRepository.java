package com.spunish.common.storage;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read/prune side only — there is no public "insert" here. Every sync event
 * is written by {@link MySqlPunishmentRepository} inside the same
 * transaction as the punishment write that caused it (task 6.1); nothing
 * else ever writes one.
 */
public interface SyncEventRepository {

    /**
     * @param sinceInclusive the poll cursor, already pulled back by the
     *                        consumer's overlap window (task 6.2) — this
     *                        method does not apply one itself.
     */
    CompletableFuture<List<SyncEvent>> pollSince(Instant sinceInclusive);

    CompletableFuture<Integer> deleteOlderThan(Instant threshold);
}
