package com.spunish.common.service;

import com.spunish.common.config.ReportSettings;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.storage.ReasonDistributionEntry;
import com.spunish.common.storage.ReportRepository;
import com.spunish.common.storage.StaffRanking;
import com.spunish.common.storage.StateBreakdown;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A query is served from cache when a fresh-enough entry exists for the same
 * (category, window[, staffer]) key, regardless of the requesting user's
 * cooldown. Only a cache miss is subject to the per-user cooldown.
 */
public final class ReportService {

    private final ReportRepository repository;
    private final ReportWindowCalculator windowCalculator = new ReportWindowCalculator();
    private final SystemClock clock;
    private final Supplier<ZoneId> zoneSupplier;
    private final Supplier<ReportSettings> settingsSupplier;

    private final Map<GeneralKey, CachedEntry<GeneralReportSummary>> generalCache = new ConcurrentHashMap<>();
    private final Map<StafferKey, CachedEntry<StafferReportSummary>> stafferCache = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastRequestByUser = new ConcurrentHashMap<>();

    public ReportService(
            ReportRepository repository,
            SystemClock clock,
            Supplier<ZoneId> zoneSupplier,
            Supplier<ReportSettings> settingsSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.zoneSupplier = zoneSupplier;
        this.settingsSupplier = settingsSupplier;
    }

    public CompletableFuture<ReportOutcome<GeneralReportSummary>> generalReport(
            UUID requestingUser, PunishmentCategory category, ReportWindow window) {
        return withCacheAndCooldown(
                requestingUser, generalCache, new GeneralKey(category, window),
                () -> computeGeneralReport(category, window));
    }

    public CompletableFuture<ReportOutcome<StafferReportSummary>> stafferReport(
            UUID requestingUser, PunishmentCategory category, ReportWindow window, UUID staffUuid) {
        return withCacheAndCooldown(
                requestingUser, stafferCache, new StafferKey(category, window, staffUuid),
                () -> computeStafferReport(category, window, staffUuid));
    }

    private CompletableFuture<GeneralReportSummary> computeGeneralReport(PunishmentCategory category, ReportWindow window) {
        WindowBounds bounds = windowCalculator.bounds(window, clock.now(), zoneSupplier.get());
        int rankingLimit = settingsSupplier.get().rankingLimit();

        CompletableFuture<Long> total = repository.countTotal(category, bounds.from(), bounds.to(), null);
        CompletableFuture<StateBreakdown> byState = repository.countByState(category, bounds.from(), bounds.to(), null);
        CompletableFuture<List<StaffRanking>> ranking =
                repository.rankByStaff(category, bounds.from(), bounds.to(), rankingLimit + 1);
        CompletableFuture<List<ReasonDistributionEntry>> reasons =
                repository.reasonDistribution(category, bounds.from(), bounds.to(), null);

        return CompletableFuture.allOf(total, byState, ranking, reasons).thenApply(ignored -> {
            List<StaffRanking> rankingRows = ranking.join();
            boolean truncated = rankingRows.size() > rankingLimit;
            List<StaffRanking> trimmedRanking = truncated ? rankingRows.subList(0, rankingLimit) : rankingRows;
            return new GeneralReportSummary(total.join(), byState.join(), trimmedRanking, truncated, reasons.join());
        });
    }

    private CompletableFuture<StafferReportSummary> computeStafferReport(
            PunishmentCategory category, ReportWindow window, UUID staffUuid) {
        WindowBounds bounds = windowCalculator.bounds(window, clock.now(), zoneSupplier.get());

        CompletableFuture<Long> total = repository.countTotal(category, bounds.from(), bounds.to(), staffUuid);
        CompletableFuture<StateBreakdown> byState = repository.countByState(category, bounds.from(), bounds.to(), staffUuid);
        CompletableFuture<List<ReasonDistributionEntry>> reasons =
                repository.reasonDistribution(category, bounds.from(), bounds.to(), staffUuid);
        CompletableFuture<List<Punishment>> recent =
                repository.mostRecentByStaff(category, bounds.from(), bounds.to(), staffUuid, 10);

        return CompletableFuture.allOf(total, byState, reasons, recent).thenApply(ignored ->
                new StafferReportSummary(total.join(), byState.join(), reasons.join(), recent.join()));
    }

    private <K, T> CompletableFuture<ReportOutcome<T>> withCacheAndCooldown(
            UUID requestingUser, Map<K, CachedEntry<T>> cache, K key, Supplier<CompletableFuture<T>> compute) {
        ReportSettings settings = settingsSupplier.get();
        Instant now = clock.now();

        CachedEntry<T> cached = cache.get(key);
        if (cached != null && cached.cachedAt().plusMillis(settings.cacheDurationMs()).isAfter(now)) {
            return CompletableFuture.completedFuture(new ReportOutcome.Ready<>(cached.value()));
        }

        Instant lastRequest = lastRequestByUser.get(requestingUser);
        if (lastRequest != null) {
            Instant readyAt = lastRequest.plusMillis(settings.cooldownMs());
            if (readyAt.isAfter(now)) {
                return CompletableFuture.completedFuture(new ReportOutcome.CooldownActive<>(Duration.between(now, readyAt)));
            }
        }

        lastRequestByUser.put(requestingUser, now);
        return compute.get().thenApply(value -> {
            cache.put(key, new CachedEntry<>(value, now));
            return new ReportOutcome.Ready<>(value);
        });
    }

    private record GeneralKey(PunishmentCategory category, ReportWindow window) {
    }

    private record StafferKey(PunishmentCategory category, ReportWindow window, UUID staffUuid) {
    }

    private record CachedEntry<T>(T value, Instant cachedAt) {
    }
}
