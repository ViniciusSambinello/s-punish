package com.spunish.common.message;

import com.spunish.common.config.YamlConfigLoader;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {

    private static final String ROOT_YAML = """
            prefix: "[P] "
            date-time:
              pattern: "dd/MM/yyyy HH:mm"
              zone: "UTC"
            duration:
              max-units: 2
              permanent-text: "forever"
              units:
                s: "s"
                m: "m"
                h: "h"
                d: "d"
                w: "w"
                mo: "mo"
                y: "y"
            messages:
              test:
                greeting: "Hello {name}!"
                unknown-token: "Hello {ghost}!"
                malformed: "<red>unclosed"
                lines:
                  - "line one"
                  - "line two"
                disabled: ""
                disabled-list: []
            """;

    private Logger logger;
    private MessageService service;
    private ConfigurationNode defaults;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        logger = Logger.getLogger("MessageServiceTest");
        Path rootFile = tempDir.resolve("messages.yml");
        Files.writeString(rootFile, ROOT_YAML);
        ConfigurationNode root = YamlConfigLoader.loadNode(rootFile, "/default/messages.yml");

        Path defaultsFile = tempDir.resolve("defaults.yml");
        defaults = YamlConfigLoader.loadNode(defaultsFile, "/default/messages.yml");

        service = new MessageService(root, defaults, logger);
    }

    @Test
    void substitutesKnownPlaceholder() {
        List<String> lines = service.renderPlain("test.greeting", Map.of("name", "World"));

        assertThat(lines).containsExactly("Hello World!");
    }

    @Test
    void leavesUnknownPlaceholderLiteral() {
        List<String> lines = service.renderPlain("test.unknown-token", Map.of());

        assertThat(lines).containsExactly("Hello {ghost}!");
    }

    @Test
    void malformedTagDegradesToPlainText() {
        List<Component> rendered = service.render("test.malformed", Map.of());

        assertThat(rendered).containsExactly(Component.text("<red>unclosed"));
    }

    @Test
    void returnsMultipleLinesInOrder() {
        List<String> lines = service.renderPlain("test.lines", Map.of());

        assertThat(lines).containsExactly("line one", "line two");
    }

    @Test
    void emptyStringMessageIsDisabled() {
        assertThat(service.renderPlain("test.disabled", Map.of())).isEmpty();
        assertThat(service.render("test.disabled", Map.of())).isEmpty();
    }

    @Test
    void emptyListMessageIsDisabled() {
        assertThat(service.renderPlain("test.disabled-list", Map.of())).isEmpty();
    }

    @Test
    void missingKeyFallsBackToBundledDefault() {
        List<String> lines = service.renderPlain("general.no-permission", Map.of());

        assertThat(lines).isNotEmpty();
        assertThat(lines.get(0)).contains("permiss");
    }

    @Test
    void prefixPlaceholderIsAvailableEverywhere() {
        List<String> lines = service.renderPlain("test.greeting", Map.of("name", "World"));

        // {prefix} is applied to every message, not just ones with other placeholders.
        assertThat(service.renderPlain("general.no-permission", Map.of()).get(0)).startsWith("[P] ");
        assertThat(lines).containsExactly("Hello World!");
    }

    @Test
    void formatsDurationUsingConfiguredLabels() {
        assertThat(service.formatDuration(java.time.Duration.ofMinutes(90))).isEqualTo("1h 30m");
    }

    @Test
    void formatsPermanentDuration() {
        assertThat(service.formatDuration(null)).isEqualTo("forever");
    }

    @Test
    void reloadWithInvalidZoneKeepsPreviousMessagesInEffect() throws IOException {
        String brokenYaml = ROOT_YAML.replace("zone: \"UTC\"", "zone: \"Not/AZone\"");
        Path brokenFile = Files.createTempFile("messages-broken", ".yml");
        Files.writeString(brokenFile, brokenYaml);
        ConfigurationNode brokenRoot = YamlConfigLoader.loadNode(brokenFile, "/default/messages.yml");

        boolean reloaded = service.reload(brokenRoot);

        assertThat(reloaded).isFalse();
        assertThat(service.renderPlain("test.greeting", Map.of("name", "World"))).containsExactly("Hello World!");

        Files.deleteIfExists(brokenFile);
    }
}
