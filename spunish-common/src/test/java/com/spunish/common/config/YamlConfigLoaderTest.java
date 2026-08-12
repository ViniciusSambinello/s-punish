package com.spunish.common.config;

import com.spunish.common.catalog.ReasonCatalogFile;
import com.spunish.common.catalog.ReasonDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YamlConfigLoaderTest {

    @Test
    void loadsBundledDefaultConfigIntoRecord(@TempDir Path tempDir) {
        Path target = tempDir.resolve("config.yml");

        SpunishConfig config = YamlConfigLoader.load(target, "/default/config.yml", SpunishConfig.class);

        assertThat(target).exists();
        assertThat(config.database().host()).isEqualTo("localhost");
        assertThat(config.database().port()).isEqualTo(3306);
        assertThat(config.database().pool().maximumPoolSize()).isEqualTo(10);
        assertThat(config.failMode().login()).isEqualTo(FailMode.DENY);
        assertThat(config.failMode().chat()).isEqualTo(FailMode.ALLOW);
        assertThat(config.retention().enabled()).isFalse();
        assertThat(config.mute().blockedCommands()).contains("msg", "tell");
        assertThat(config.durationLimits()).containsEntry("trial", "7d");
    }

    @Test
    void loadsBundledDefaultReasonsIntoRecord(@TempDir Path tempDir) {
        Path target = tempDir.resolve("reasons.yml");

        ReasonCatalogFile file = YamlConfigLoader.load(target, "/default/reasons.yml", ReasonCatalogFile.class);

        assertThat(target).exists();
        List<ReasonDefinition> banReasons = file.reasons().get("ban");
        assertThat(banReasons).isNotNull();
        assertThat(banReasons).anySatisfy(reason -> {
            assertThat(reason.id()).isEqualTo("hacking");
            assertThat(reason.displayName()).isEqualTo("Use of illegal client");
            assertThat(reason.duration()).isEqualTo("30d");
            assertThat(reason.permission()).isEqualTo("spunish.reason.ban.hacking");
        });
    }

    @Test
    void doesNotOverwriteExistingFile(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("config.yml");
        java.nio.file.Files.writeString(target, "database:\n  host: custom-host\n  port: 3306\n  database: spunish\n  user: spunish\n  password: x\n  table-prefix: sp_\n  use-ssl: false\n  pool:\n    maximum-pool-size: 1\n    minimum-idle: 1\n    connection-timeout-ms: 1\n    max-lifetime-ms: 1\n  query-timeout-ms: 1\nserver:\n  id: x\nsync:\n  poll-interval-ms: 1\n  overlap-ms: 1\n  event-retention-ms: 1\nfail-mode:\n  login: ALLOW\n  chat: ALLOW\nretention:\n  enabled: false\n  retention-days: 1\nmute:\n  blocked-commands: []\n  warning-cooldown-ms: 1\nreport:\n  cooldown-ms: 1\n  cache-duration-ms: 1\n  ranking-limit: 1\nduration-limits: {}\n");

        SpunishConfig config = YamlConfigLoader.load(target, "/default/config.yml", SpunishConfig.class);

        assertThat(config.database().host()).isEqualTo("custom-host");
    }
}
