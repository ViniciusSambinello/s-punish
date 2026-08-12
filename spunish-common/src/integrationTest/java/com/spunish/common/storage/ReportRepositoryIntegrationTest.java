package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ReportRepositoryIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider provider;
    private static IoExecutor ioExecutor;
    private static MySqlPunishmentRepository punishmentRepository;
    private static MySqlReportRepository reportRepository;

    @BeforeAll
    static void setUp() throws Exception {
        provider = new DatabaseConnectionProvider(new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "", false,
                new PoolSettings(10, 2, 5_000, 60_000), 5_000));
        TableNames tables = new TableNames("");
        new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();
        ioExecutor = new IoExecutor(Duration.ofSeconds(10));
        MySqlSyncEventRepository syncEvents = new MySqlSyncEventRepository(provider.dataSource(), ioExecutor, tables);
        punishmentRepository = new MySqlPunishmentRepository(provider.dataSource(), ioExecutor, tables, syncEvents);
        reportRepository = new MySqlReportRepository(provider.dataSource(), ioExecutor, tables);
    }

    @AfterAll
    static void tearDown() {
        ioExecutor.close();
        provider.close();
    }

    @Test
    void revokedPunishmentsStillCountTowardTheTotalAndTheRevocationRateReflectsThem() throws Exception {
        PunishmentCategory category = PunishmentCategory.BAN;
        UUID staffUuid = UUID.randomUUID();
        Actor staffer = new PlayerActor(staffUuid, "Staffer");
        Instant now = Instant.now();
        Instant from = now.minusSeconds(3600);
        Instant to = now.plusSeconds(3600);

        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Punishment applied = punishmentRepository.insert(new InsertPunishmentCommand(
                    category, UUID.randomUUID(), "T" + i, staffer, "reason", "Reason", now, now.plusSeconds(7200), "server-a")).get();
            ids.add(applied.id());
        }
        for (int i = 0; i < 3; i++) {
            punishmentRepository.revoke(ids.get(i), ConsoleActor.INSTANCE, now, null, "server-a").get();
        }

        long total = reportRepository.countTotal(category, from, to, staffUuid).get();
        StateBreakdown byState = reportRepository.countByState(category, from, to, staffUuid).get();

        assertThat(total).isEqualTo(10);
        assertThat(byState.total()).isEqualTo(10);
        assertThat(byState.revoked()).isEqualTo(3);
        assertThat(byState.revocationRatePercent()).isEqualTo(30.0);
    }

    @Test
    void rankByStaffOrdersDescendingByCountAndCarriesTheStaffersFrozenName() throws Exception {
        PunishmentCategory category = PunishmentCategory.MUTE;
        Instant now = Instant.now();
        Actor topStaffer = new PlayerActor(UUID.randomUUID(), "TopStaffer");
        Actor otherStaffer = new PlayerActor(UUID.randomUUID(), "OtherStaffer");

        for (int i = 0; i < 4; i++) {
            punishmentRepository.insert(new InsertPunishmentCommand(
                    category, UUID.randomUUID(), "T" + i, topStaffer, "reason", "Reason", now, null, "server-a")).get();
        }
        punishmentRepository.insert(new InsertPunishmentCommand(
                category, UUID.randomUUID(), "T5", otherStaffer, "reason", "Reason", now, null, "server-a")).get();

        List<StaffRanking> ranking = reportRepository.rankByStaff(category, now.minusSeconds(60), now.plusSeconds(60), 10).get();

        assertThat(ranking).isNotEmpty();
        StaffRanking first = ranking.get(0);
        assertThat(first.count()).isEqualTo(4);
        assertThat(first.staffName()).isEqualTo("TopStaffer");
    }

    @Test
    void reasonDistributionOrdersDescendingByCount() throws Exception {
        PunishmentCategory category = PunishmentCategory.BAN;
        Instant now = Instant.now();
        Actor staffer = new PlayerActor(UUID.randomUUID(), "Staffer");

        for (int i = 0; i < 3; i++) {
            punishmentRepository.insert(new InsertPunishmentCommand(
                    category, UUID.randomUUID(), "T" + i, staffer, "hacking", "Illegal client", now, null, "server-a")).get();
        }
        punishmentRepository.insert(new InsertPunishmentCommand(
                category, UUID.randomUUID(), "T4", staffer, "griefing", "Griefing", now, null, "server-a")).get();

        List<ReasonDistributionEntry> distribution =
                reportRepository.reasonDistribution(category, now.minusSeconds(60), now.plusSeconds(60), null).get();

        assertThat(distribution).isNotEmpty();
        assertThat(distribution.get(0).reasonId()).isEqualTo("hacking");
        assertThat(distribution.get(0).count()).isEqualTo(3);
    }
}
