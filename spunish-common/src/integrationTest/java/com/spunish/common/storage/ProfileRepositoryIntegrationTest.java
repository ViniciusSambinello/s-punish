package com.spunish.common.storage;

import com.spunish.common.config.DatabaseSettings;
import com.spunish.common.config.PoolSettings;
import com.spunish.common.domain.PlayerProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ProfileRepositoryIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static DatabaseConnectionProvider provider;
    private static IoExecutor ioExecutor;
    private static MySqlProfileRepository repository;

    @BeforeAll
    static void setUp() throws Exception {
        provider = new DatabaseConnectionProvider(new DatabaseSettings(
                MYSQL.getHost(), MYSQL.getFirstMappedPort(), MYSQL.getDatabaseName(),
                MYSQL.getUsername(), MYSQL.getPassword(), "", false,
                new PoolSettings(10, 2, 5_000, 60_000), 5_000));
        TableNames tables = new TableNames("");
        new SchemaMigrator(provider.dataSource(), tables, Logger.getLogger("t")).migrate();
        ioExecutor = new IoExecutor(Duration.ofSeconds(10));
        repository = new MySqlProfileRepository(provider.dataSource(), ioExecutor, tables);
    }

    @AfterAll
    static void tearDown() {
        ioExecutor.close();
        provider.close();
    }

    @Test
    void aRenamedPlayerIsResolvedByTheirNewNameNotTheOldOne() throws Exception {
        UUID uuid = UUID.randomUUID();
        Instant firstSeen = Instant.now().minusSeconds(120);
        Instant renamedAt = Instant.now();

        repository.upsert(new PlayerProfile(uuid, "OldName", firstSeen)).get();
        repository.upsert(new PlayerProfile(uuid, "NewName", renamedAt)).get();

        Optional<PlayerProfile> byUuid = repository.findByUuid(uuid).get();
        assertThat(byUuid).isPresent();
        assertThat(byUuid.get().name()).isEqualTo("NewName");

        assertThat(repository.findByName("NewName").get()).isPresent();
        assertThat(repository.findByName("OldName").get()).isEmpty();
    }

    @Test
    void aNameReusedByAnotherAccountResolvesToWhoeverUsedItMostRecently() throws Exception {
        String sharedName = "Shared" + UUID.randomUUID().toString().substring(0, 6);
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        Instant earlier = Instant.now().minusSeconds(3600);
        Instant later = Instant.now();

        repository.upsert(new PlayerProfile(firstOwner, sharedName, earlier)).get();
        repository.upsert(new PlayerProfile(secondOwner, sharedName, later)).get();

        Optional<PlayerProfile> resolved = repository.findByName(sharedName).get();

        assertThat(resolved).isPresent();
        assertThat(resolved.get().uuid()).isEqualTo(secondOwner);
    }
}
