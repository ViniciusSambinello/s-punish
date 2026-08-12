package com.spunish.common.message;

import com.spunish.common.config.YamlConfigLoader;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemActor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentPlaceholdersTest {

    private MessageService service(Path tempDir) {
        Path root = tempDir.resolve("messages.yml");
        ConfigurationNode node = YamlConfigLoader.loadNode(root, "/default/messages.yml");
        ConfigurationNode defaults = YamlConfigLoader.loadBundledDefaults("/default/messages.yml");
        return new MessageService(node, defaults, Logger.getLogger("test"));
    }

    @Test
    void buildsPlaceholdersForAnActivePermanentBan(@TempDir Path tempDir) {
        Punishment punishment = new Punishment(
                1, "ABC12345", PunishmentCategory.BAN, UUID.randomUUID(), "Target",
                ConsoleActor.INSTANCE, "hacking", "Hacking", Instant.parse("2026-01-01T00:00:00Z"),
                null, "server-1", null, null, null);

        Map<String, String> placeholders = PunishmentPlaceholders.build(punishment, service(tempDir), Instant.parse("2026-01-02T00:00:00Z"));

        assertThat(placeholders.get("target")).isEqualTo("Target");
        assertThat(placeholders.get("actor")).isEqualTo("Console");
        assertThat(placeholders.get("reason-display")).isEqualTo("Hacking");
        assertThat(placeholders.get("punishment-id")).isEqualTo("ABC12345");
        assertThat(placeholders.get("duration")).isEqualTo("permanent");
        assertThat(placeholders.get("duration-remaining")).isEqualTo("permanent");
        assertThat(placeholders).doesNotContainKey("revoker");
    }

    @Test
    void buildsPlaceholdersForARevokedTemporaryMute(@TempDir Path tempDir) {
        Punishment punishment = new Punishment(
                2, "DEF67890", PunishmentCategory.MUTE, UUID.randomUUID(), "Target",
                ConsoleActor.INSTANCE, "spam", "Spam", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T02:00:00Z"), "server-1",
                SystemActor.INSTANCE, Instant.parse("2026-01-01T01:00:00Z"), "appeal accepted");

        Map<String, String> placeholders = PunishmentPlaceholders.build(punishment, service(tempDir), Instant.parse("2026-01-01T00:30:00Z"));

        assertThat(placeholders.get("duration")).isEqualTo("2h");
        assertThat(placeholders.get("duration-remaining")).isEqualTo("1h 30m");
        assertThat(placeholders.get("revoker")).isEqualTo("System");
        assertThat(placeholders.get("revoke-reason")).isEqualTo("appeal accepted");
    }

    @Test
    void remainingDurationNeverGoesNegativeForAnAlreadyExpiredPunishment(@TempDir Path tempDir) {
        Punishment punishment = new Punishment(
                3, "GHI11111", PunishmentCategory.MUTE, UUID.randomUUID(), "Target",
                ConsoleActor.INSTANCE, "spam", "Spam", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"), "server-1", null, null, null);

        Map<String, String> placeholders = PunishmentPlaceholders.build(punishment, service(tempDir), Instant.parse("2026-01-01T05:00:00Z"));

        assertThat(placeholders.get("duration-remaining")).isEqualTo("0s");
    }
}
