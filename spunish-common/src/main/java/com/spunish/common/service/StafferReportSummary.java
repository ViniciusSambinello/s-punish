package com.spunish.common.service;

import com.spunish.common.domain.Punishment;
import com.spunish.common.storage.ReasonDistributionEntry;
import com.spunish.common.storage.StateBreakdown;

import java.util.List;

public record StafferReportSummary(
        long total,
        StateBreakdown byState,
        List<ReasonDistributionEntry> reasonDistribution,
        List<Punishment> recentPunishments) {
}
