package com.spunish.common.storage;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@code from == null} means "since the beginning"; {@code staffUuid == null}
 * on the filtered methods means "the whole staff", not "no one".
 */
public interface ReportRepository {

    CompletableFuture<Long> countTotal(PunishmentCategory category, Instant from, Instant to, UUID staffUuid);

    /**
     * Active/expired are decided by the database's own clock at query time,
     * independent of the {@code [from, to]} window those punishments were created in.
     */
    CompletableFuture<StateBreakdown> countByState(PunishmentCategory category, Instant from, Instant to, UUID staffUuid);

    CompletableFuture<List<StaffRanking>> rankByStaff(PunishmentCategory category, Instant from, Instant to, int limit);

    CompletableFuture<List<ReasonDistributionEntry>> reasonDistribution(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid);

    CompletableFuture<List<Punishment>> mostRecentByStaff(
            PunishmentCategory category, Instant from, Instant to, UUID staffUuid, int limit);
}
