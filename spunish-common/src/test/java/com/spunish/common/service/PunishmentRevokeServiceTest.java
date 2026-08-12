package com.spunish.common.service;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.platform.ServerIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentRevokeServiceTest {

    private InMemoryPunishmentRepository repository;
    private FakePermissionChecker permissions;
    private FixedSystemClock clock;
    private PunishmentIssueService issueService;
    private PunishmentRevokeService revokeService;

    private final UUID staffUuid = UUID.randomUUID();
    private final UUID targetUuid = UUID.randomUUID();
    private final PlayerActor staffActor = new PlayerActor(staffUuid, "Staffer");
    private final PunishmentTarget target = new PunishmentTarget(targetUuid, "Target");

    @BeforeEach
    void setUp() throws Exception {
        clock = new FixedSystemClock(Instant.parse("2026-01-01T00:00:00Z"));
        repository = new InMemoryPunishmentRepository(clock);
        permissions = new FakePermissionChecker();
        issueService = new PunishmentIssueService(
                repository, clock, (ServerIdentity) () -> "test-server", permissions, Map::of);
        revokeService = new PunishmentRevokeService(repository, clock, (ServerIdentity) () -> "test-server", permissions);

        permissions.grant(staffActor, "spunish.punish.ban");
    }

    private Reason banReason(String duration) {
        return new Reason("hacking", PunishmentCategory.BAN, "Hacking",
                new com.spunish.common.duration.DurationParser().parse(duration).orElseThrow(), null, null, null);
    }

    @Test
    void revocationFailsWhenNoActivePunishmentExists() throws Exception {
        permissions.grant(staffActor, "spunish.unpunish.ban");

        RevokeResult result = revokeService.revoke(targetUuid, PunishmentCategory.BAN, staffActor, null).get();

        assertThat(result).isInstanceOf(RevokeResult.NoActivePunishment.class);
    }

    @Test
    void revocationFailsWithoutPermission() throws Exception {
        issueService.issue(new IssueCommand(target, staffActor, PunishmentCategory.BAN, banReason("1d"), null)).get();

        RevokeResult result = revokeService.revoke(targetUuid, PunishmentCategory.BAN, staffActor, null).get();

        assertThat(result).isInstanceOf(RevokeResult.PermissionDenied.class);
    }

    @Test
    void consoleCanRevokeAndIsRecordedAsTheRevoker() throws Exception {
        issueService.issue(new IssueCommand(target, staffActor, PunishmentCategory.BAN, banReason("1d"), null)).get();

        RevokeResult result = revokeService.revoke(targetUuid, PunishmentCategory.BAN, ConsoleActor.INSTANCE, "apelação aceita").get();

        assertThat(result).isInstanceOf(RevokeResult.Success.class);
        var revoked = ((RevokeResult.Success) result).revoked();
        assertThat(revoked.revoker()).isEqualTo(ConsoleActor.INSTANCE);
        assertThat(revoked.revokeReason()).isEqualTo("apelação aceita");
    }

    @Test
    void originalRecordIsPreservedAfterRevocation() throws Exception {
        var applied = (IssueResult.Success) issueService.issue(
                new IssueCommand(target, staffActor, PunishmentCategory.BAN, banReason("1d"), null)).get();
        permissions.grant(staffActor, "spunish.unpunish.ban");

        RevokeResult result = revokeService.revoke(targetUuid, PunishmentCategory.BAN, staffActor, "reason").get();

        var revoked = ((RevokeResult.Success) result).revoked();
        assertThat(revoked.reasonId()).isEqualTo(applied.applied().reasonId());
        assertThat(revoked.createdAt()).isEqualTo(applied.applied().createdAt());
        assertThat(revoked.actor()).isEqualTo(applied.applied().actor());
        assertThat(revoked.expiresAt()).isEqualTo(applied.applied().expiresAt());
    }
}
