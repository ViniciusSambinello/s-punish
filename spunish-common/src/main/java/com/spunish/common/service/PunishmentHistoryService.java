package com.spunish.common.service;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentState;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.storage.PunishmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Pages are loaded on demand (one repository call per page, not "load
 * everything and paginate in memory") and resolved into {@link PunishmentState}
 * against the current instant before being handed back.
 */
public final class PunishmentHistoryService {

    private final PunishmentRepository repository;
    private final SystemClock clock;

    public PunishmentHistoryService(PunishmentRepository repository, SystemClock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CompletableFuture<HistoryPage> page(UUID targetUuid, Optional<PunishmentCategory> category, int page, int pageSize) {
        // Over-fetch by one to know whether a next page exists without a separate COUNT
        // query — offset is computed from the real pageSize, not the padded limit.
        int offset = page * pageSize;
        return repository.findHistory(targetUuid, category, pageSize + 1, offset)
                .thenApply(rows -> {
                    boolean hasNext = rows.size() > pageSize;
                    List<HistoryEntry> entries = rows.stream()
                            .limit(pageSize)
                            .map(p -> new HistoryEntry(p, PunishmentState.resolve(p, clock.now())))
                            .toList();
                    return new HistoryPage(entries, page, pageSize, hasNext);
                });
    }
}
