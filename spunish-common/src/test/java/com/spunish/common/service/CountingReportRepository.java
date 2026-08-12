package com.spunish.common.service;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.storage.ReasonDistributionEntry;
import com.spunish.common.storage.ReportRepository;
import com.spunish.common.storage.StaffRanking;
import com.spunish.common.storage.StateBreakdown;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts how many times an aggregation actually ran, to prove cache/cooldown short-circuit it. */
final class CountingReportRepository implements ReportRepository {

    final AtomicInteger aggregationCalls = new AtomicInteger();

    @Override
    public CompletableFuture<Long> countTotal(PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        aggregationCalls.incrementAndGet();
        return CompletableFuture.completedFuture(42L);
    }

    @Override
    public CompletableFuture<StateBreakdown> countByState(PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        return CompletableFuture.completedFuture(new StateBreakdown(30, 5, 7));
    }

    @Override
    public CompletableFuture<List<StaffRanking>> rankByStaff(PunishmentCategory category, Instant from, Instant to, int limit) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<List<ReasonDistributionEntry>> reasonDistribution(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<List<Punishment>> mostRecentByStaff(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid, int limit) {
        return CompletableFuture.completedFuture(List.of());
    }
}
