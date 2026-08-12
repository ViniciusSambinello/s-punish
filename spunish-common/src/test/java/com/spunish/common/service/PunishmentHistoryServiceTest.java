package com.spunish.common.service;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentState;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.duration.DurationParser;
import com.spunish.common.platform.ServerIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentHistoryServiceTest {

    private InMemoryPunishmentRepository repository;
    private FixedSystemClock clock;
    private PunishmentIssueService issueService;
    private PunishmentHistoryService historyService;

    private final UUID targetUuid = UUID.randomUUID();
    private final PunishmentTarget target = new PunishmentTarget(targetUuid, "Target");
    private final PlayerActor staffActor = new PlayerActor(UUID.randomUUID(), "Staffer");

    @BeforeEach
    void setUp() throws Exception {
        clock = new FixedSystemClock(Instant.parse("2026-01-01T00:00:00Z"));
        repository = new InMemoryPunishmentRepository(clock);
        FakePermissionChecker permissions = new FakePermissionChecker();
        permissions.grant(staffActor, "spunish.punish.ban");
        permissions.grant(staffActor, "spunish.punish.override");
        issueService = new PunishmentIssueService(
                repository, clock, (ServerIdentity) () -> "test-server", permissions, Map::of);
        historyService = new PunishmentHistoryService(repository, clock);

        for (int i = 0; i < 5; i++) {
            Reason reason = new Reason("r" + i, PunishmentCategory.BAN, "Reason " + i,
                    new DurationParser().parse("1d").orElseThrow(), null, null, null);
            issueService.issue(new IssueCommand(target, staffActor, PunishmentCategory.BAN, reason, null)).get();
            clock.advance(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void pageSizeCorrectlyBoundsResultsWithoutSkippingRowsAcrossPages() throws Exception {
        HistoryPage firstPage = historyService.page(targetUuid, Optional.empty(), 0, 2).get();
        HistoryPage secondPage = historyService.page(targetUuid, Optional.empty(), 1, 2).get();
        HistoryPage thirdPage = historyService.page(targetUuid, Optional.empty(), 2, 2).get();

        assertThat(firstPage.entries()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.entries()).hasSize(2);
        assertThat(secondPage.hasNext()).isTrue();
        assertThat(thirdPage.entries()).hasSize(1);
        assertThat(thirdPage.hasNext()).isFalse();

        // No punishment id appears on two different pages.
        var allIds = new java.util.HashSet<Long>();
        for (var entry : firstPage.entries()) allIds.add(entry.punishment().id());
        for (var entry : secondPage.entries()) allIds.add(entry.punishment().id());
        for (var entry : thirdPage.entries()) allIds.add(entry.punishment().id());
        assertThat(allIds).hasSize(5);
    }

    @Test
    void entriesAreResolvedToPunishmentState() throws Exception {
        HistoryPage page = historyService.page(targetUuid, Optional.empty(), 0, 5).get();

        assertThat(page.entries()).allSatisfy(entry ->
                assertThat(entry.state()).isInstanceOfAny(PunishmentState.Active.class, PunishmentState.Revoked.class));
    }
}
