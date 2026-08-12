package com.spunish.common.config;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlConfigLoader {

    private static final ObjectMapper.Factory OBJECT_MAPPER_FACTORY = ObjectMapper.factory();

    private YamlConfigLoader() {
    }

    public static <T> T load(Path target, String defaultResourcePath, Class<T> type) {
        ensureFileExists(target, defaultResourcePath);
        try {
            ConfigurationNode node = loaderFor(target).load();
            T value = node.get(type);
            if (value == null) {
                throw new ConfigLoadException("Empty configuration file: " + target);
            }
            return value;
        } catch (ConfigurateException e) {
            throw new ConfigLoadException("Could not load " + target + ": " + e.getMessage(), e);
        }
    }

    public static ConfigurationNode loadNode(Path target, String defaultResourcePath) {
        ensureFileExists(target, defaultResourcePath);
        try {
            return loaderFor(target).load();
        } catch (ConfigurateException e) {
            throw new ConfigLoadException("Could not parse " + target + ": " + e.getMessage(), e);
        }
    }

    /**
     * A message key missing from the on-disk {@code messages.yml} falls back
     * to the shipped bundled default, so a config file that predates a newer
     * key still resolves to correct text.
     */
    public static ConfigurationNode loadBundledDefaults(String resourcePath) {
        try (InputStream in = YamlConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled default resource: " + resourcePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .source(() -> reader)
                    .defaultOptions(options -> options.serializers(builder -> builder.registerAnnotatedObjects(OBJECT_MAPPER_FACTORY)))
                    .build();
            return loader.load();
        } catch (IOException e) {
            throw new ConfigLoadException("Could not parse bundled resource " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    private static YamlConfigurationLoader loaderFor(Path target) {
        return YamlConfigurationLoader.builder()
                .path(target)
                .defaultOptions(options -> options.serializers(builder -> builder.registerAnnotatedObjects(OBJECT_MAPPER_FACTORY)))
                .build();
    }

    private static void ensureFileExists(Path target, String defaultResourcePath) {
        if (Files.exists(target)) {
            return;
        }
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (InputStream in = YamlConfigLoader.class.getResourceAsStream(defaultResourcePath)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled default resource: " + defaultResourcePath);
                }
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + target, e);
        }
    }
}
