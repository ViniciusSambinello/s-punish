package com.spunish.common.service;

import com.spunish.common.storage.ReasonDistributionEntry;
import com.spunish.common.storage.StaffRanking;
import com.spunish.common.storage.StateBreakdown;

import java.util.List;

public record GeneralReportSummary(
        long total,
        StateBreakdown byState,
        List<StaffRanking> ranking,
        boolean rankingTruncated,
        List<ReasonDistributionEntry> reasonDistribution) {
}
