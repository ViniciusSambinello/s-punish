package com.spunish.common.storage;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SyncEventRepository {

    CompletableFuture<List<SyncEvent>> pollSince(Instant sinceInclusive);

    CompletableFuture<Integer> deleteOlderThan(Instant threshold);
}
