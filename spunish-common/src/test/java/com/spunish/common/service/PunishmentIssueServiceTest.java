package com.spunish.common.service;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.duration.PunishmentDuration;
import com.spunish.common.platform.ServerIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentIssueServiceTest {

    private InMemoryPunishmentRepository repository;
    private FakePermissionChecker permissions;
    private FixedSystemClock clock;
    private PunishmentIssueService service;
    private Map<String, String> durationLimits = Map.of();

    private final UUID staffUuid = UUID.randomUUID();
    private final UUID targetUuid = UUID.randomUUID();
    private final PlayerActor staffActor = new PlayerActor(staffUuid, "Staffer");
    private final PunishmentTarget target = new PunishmentTarget(targetUuid, "Target");

    @BeforeEach
    void setUp() {
        clock = new FixedSystemClock(Instant.parse("2026-01-01T00:00:00Z"));
        repository = new InMemoryPunishmentRepository(clock);
        permissions = new FakePermissionChecker();
        service = new PunishmentIssueService(
                repository, clock, (ServerIdentity) () -> "test-server", permissions, () -> durationLimits);

        permissions.grant(staffActor, "spunish.punish.ban");
        permissions.grant(staffActor, "spunish.punish.mute");
    }

    private Reason reason(String id, PunishmentCategory category, String duration) {
        return new Reason(id, category, "Display " + id, new com.spunish.common.duration.DurationParser().parse(duration).orElseThrow(),
                null, null, null);
    }

    private IssueResult issue(PunishmentTarget target, com.spunish.common.domain.Actor actor, PunishmentCategory category, Reason reason)
            throws ExecutionException, InterruptedException {
        return service.issue(new IssueCommand(target, actor, category, reason, null)).get();
    }

    @Test
    void selfPunishmentIsDeniedWithoutPermission() throws Exception {
        PunishmentTarget self = new PunishmentTarget(staffUuid, "Staffer");
        IssueResult result = issue(self, staffActor, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.SelfPunishDenied.class);
    }

    @Test
    void selfPunishmentIsAllowedWithPermission() throws Exception {
        permissions.grant(staffActor, "spunish.punish.self");
        PunishmentTarget self = new PunishmentTarget(staffUuid, "Staffer");
        IssueResult result = issue(self, staffActor, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.Success.class);
    }

    @Test
    void exemptTargetIsProtectedWithoutOverride() throws Exception {
        permissions.grantToTarget("spunish.exempt.ban");
        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.TargetExempt.class);
    }

    @Test
    void exemptTargetCanStillBePunishedWithOverride() throws Exception {
        permissions.grantToTarget("spunish.exempt.ban");
        permissions.grant(staffActor, "spunish.punish.override");
        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.Success.class);
    }

    @Test
    void duplicateActivePunishmentIsRefusedWithoutOverride() throws Exception {
        Reason banReason = reason("hacking", PunishmentCategory.BAN, "1d");
        issue(target, staffActor, PunishmentCategory.BAN, banReason);

        IssueResult second = issue(target, staffActor, PunishmentCategory.BAN, banReason);

        assertThat(second).isInstanceOf(IssueResult.AlreadyActive.class);
    }

    @Test
    void overrideRevokesThePreviousPunishmentAndAppliesTheNewOne() throws Exception {
        Reason banReason = reason("hacking", PunishmentCategory.BAN, "1h");
        IssueResult first = issue(target, staffActor, PunishmentCategory.BAN, banReason);
        long firstId = ((IssueResult.Success) first).applied().id();

        permissions.grant(staffActor, "spunish.punish.override");
        Reason strongerReason = reason("cheating", PunishmentCategory.BAN, "7d");
        IssueResult second = issue(target, staffActor, PunishmentCategory.BAN, strongerReason);

        assertThat(second).isInstanceOf(IssueResult.Success.class);
        IssueResult.Success success = (IssueResult.Success) second;
        assertThat(success.revokedByOverride()).isNotNull();
        assertThat(success.revokedByOverride().id()).isEqualTo(firstId);
        assertThat(success.revokedByOverride().isRevoked()).isTrue();
        assertThat(success.revokedByOverride().revoker()).isInstanceOf(com.spunish.common.domain.SystemActor.class);

        // Both remain visible in history.
        var history = repository.findHistory(targetUuid, java.util.Optional.of(PunishmentCategory.BAN), 10, 0).get();
        assertThat(history).hasSize(2);
    }

    @Test
    void durationAboveLimitIsRefused() throws Exception {
        permissions.grant(staffActor, "spunish.limit.trial");
        durationLimits = Map.of("trial", "7d");
        Reason longReason = reason("hacking", PunishmentCategory.BAN, "30d");

        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, longReason);

        assertThat(result).isInstanceOf(IssueResult.DurationLimitExceeded.class);
    }

    @Test
    void permanentAboveLimitIsRefused() throws Exception {
        permissions.grant(staffActor, "spunish.limit.trial");
        durationLimits = Map.of("trial", "7d");
        Reason permanentReason = reason("hacking", PunishmentCategory.BAN, "n");

        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, permanentReason);

        assertThat(result).isInstanceOf(IssueResult.DurationLimitExceeded.class);
    }

    @Test
    void authorWithoutMatchingLimitHasNoRestriction() throws Exception {
        durationLimits = Map.of("trial", "7d");
        Reason longReason = reason("hacking", PunishmentCategory.BAN, "30d");

        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, longReason);

        assertThat(result).isInstanceOf(IssueResult.Success.class);
    }

    @Test
    void differentCategoriesCoexistForTheSameTarget() throws Exception {
        IssueResult banResult = issue(target, staffActor, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));
        IssueResult muteResult = issue(target, staffActor, PunishmentCategory.MUTE, reason("spam", PunishmentCategory.MUTE, "1h"));

        assertThat(banResult).isInstanceOf(IssueResult.Success.class);
        assertThat(muteResult).isInstanceOf(IssueResult.Success.class);
    }

    @Test
    void consoleActorBypassesSelfAndOverridePermissionChecks() throws Exception {
        IssueResult result = issue(target, ConsoleActor.INSTANCE, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.Success.class);
    }

    @Test
    void categoryPermissionDeniedWhenAuthorLacksIt() throws Exception {
        PlayerActor limited = new PlayerActor(UUID.randomUUID(), "Limited");
        IssueResult result = issue(target, limited, PunishmentCategory.BAN, reason("hacking", PunishmentCategory.BAN, "1d"));

        assertThat(result).isInstanceOf(IssueResult.CategoryPermissionDenied.class);
    }

    @Test
    void restrictedReasonRequiresItsOwnPermission() throws Exception {
        Reason restricted = new Reason("hacking", PunishmentCategory.BAN, "Hacking",
                new com.spunish.common.duration.DurationParser().parse("1d").orElseThrow(),
                null, null, "spunish.reason.ban.hacking");

        IssueResult result = issue(target, staffActor, PunishmentCategory.BAN, restricted);
        assertThat(result).isInstanceOf(IssueResult.ReasonPermissionDenied.class);

        permissions.grant(staffActor, "spunish.reason.ban.hacking");
        IssueResult secondAttempt = issue(target, staffActor, PunishmentCategory.BAN, restricted);
        assertThat(secondAttempt).isInstanceOf(IssueResult.Success.class);
    }
}
