package com.spunish.common.message;

import com.spunish.common.domain.PunishmentState;
import com.spunish.common.duration.DurationFormatter;
import com.spunish.common.duration.PunishmentDuration;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Falls back to the bundled default message for a key missing from the
 * on-disk {@code messages.yml}. A reload with a syntactically broken file
 * leaves the previous messages in effect.
 */
public final class MessageService {

    private final ConfigurationNode defaults;
    private final AtomicReference<MessagesSnapshot> snapshot;
    private final PlaceholderResolver placeholderResolver = new PlaceholderResolver();
    private final MiniMessageRenderer renderer;
    private final DurationFormatter durationFormatter = new DurationFormatter();
    private final Logger logger;
    private final Set<String> warnedMissingKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> warnedUnknownPlaceholders = ConcurrentHashMap.newKeySet();

    public MessageService(ConfigurationNode initialRoot, ConfigurationNode defaults, Logger logger) {
        this.defaults = defaults;
        this.snapshot = new AtomicReference<>(MessagesSnapshot.from(initialRoot));
        this.logger = logger;
        this.renderer = new MiniMessageRenderer((key, error) ->
                logger.warning("Malformed MiniMessage tag in message '" + key + "': " + error.getMessage()));
    }

    /**
     * @return {@code false} if {@code newRoot} is invalid (for example an
     * out-of-range time zone) — the previous messages stay in effect.
     */
    public boolean reload(ConfigurationNode newRoot) {
        try {
            snapshot.set(MessagesSnapshot.from(newRoot));
            return true;
        } catch (RuntimeException invalid) {
            logger.warning("Keeping previous messages.yml in effect; reload failed: " + invalid.getMessage());
            return false;
        }
    }

    /**
     * Empty when the message is disabled (blank text or empty list) — callers
     * should send nothing in that case, not an empty component.
     */
    public List<Component> render(String key, Map<String, String> placeholders) {
        List<Component> out = new ArrayList<>();
        for (String substituted : substitutedLines(key, placeholders)) {
            out.add(renderer.render(key, substituted));
        }
        return out;
    }

    public List<String> renderPlain(String key, Map<String, String> placeholders) {
        return substitutedLines(key, placeholders);
    }

    public String formatDuration(Duration remaining) {
        return durationFormatter.format(remaining, snapshot.get().durationFormat());
    }

    public String formatDuration(PunishmentDuration duration) {
        return durationFormatter.format(duration, snapshot.get().durationFormat());
    }

    public String formatInstant(Instant instant) {
        return snapshot.get().dateTimeFormatter().format(instant);
    }

    /**
     * The {@code active}/{@code expired}/{@code revoked} label a punishment's
     * current state is always shown as (history and reporting specs) — the raw
     * MiniMessage source, meant to be embedded as a {@code {state}} placeholder
     * value and rendered together with the message that embeds it.
     */
    public String stateLabelText(PunishmentState state) {
        String key = switch (state) {
            case PunishmentState.Active ignored -> "active";
            case PunishmentState.Expired ignored -> "expired";
            case PunishmentState.Revoked ignored -> "revoked";
        };
        ConfigurationNode node = snapshot.get().root().node("state", key);
        if (isAbsent(node)) {
            node = defaults.node("state", key);
        }
        String text = node.getString();
        return text != null ? text : "";
    }

    /**
     * The configured zone also defines the calendar boundaries used for
     * daily/weekly/monthly report windows.
     */
    public ZoneId zone() {
        return snapshot.get().dateTimeFormatter().zone();
    }

    private List<String> substitutedLines(String key, Map<String, String> placeholders) {
        Map<String, String> withPrefix = withPrefix(placeholders);
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines(key)) {
            lines.add(placeholderResolver.resolve(raw, withPrefix, token -> warnUnknownPlaceholder(key, token)));
        }
        return lines;
    }

    private Map<String, String> withPrefix(Map<String, String> placeholders) {
        if (placeholders.containsKey("prefix")) {
            return placeholders;
        }
        Map<String, String> copy = new HashMap<>(placeholders);
        copy.put("prefix", snapshot.get().prefix());
        return copy;
    }

    private List<String> rawLines(String dotPath) {
        ConfigurationNode node = messageNode(snapshot.get().root(), dotPath);
        if (isAbsent(node)) {
            ConfigurationNode fallback = messageNode(defaults, dotPath);
            if (!isAbsent(fallback) && warnedMissingKeys.add(dotPath)) {
                logger.warning("Missing message key '" + dotPath + "'; using the built-in default");
            }
            node = fallback;
        }
        return linesOf(node);
    }

    private static ConfigurationNode messageNode(ConfigurationNode root, String dotPath) {
        ConfigurationNode current = root.node("messages");
        for (String part : dotPath.split("\\.")) {
            current = current.node(part);
        }
        return current;
    }

    private static boolean isAbsent(ConfigurationNode node) {
        return node.virtual() || node.isNull();
    }

    private static List<String> linesOf(ConfigurationNode node) {
        if (isAbsent(node)) {
            return List.of();
        }
        if (node.isList()) {
            List<String> lines = new ArrayList<>();
            for (ConfigurationNode child : node.childrenList()) {
                String value = child.getString();
                if (value != null) {
                    lines.add(value);
                }
            }
            return lines;
        }
        String value = node.getString();
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return List.of(value);
    }

    private void warnUnknownPlaceholder(String key, String token) {
        if (warnedUnknownPlaceholders.add(key + "#" + token)) {
            logger.warning("Unknown placeholder '{" + token + "}' in message '" + key + "'");
        }
    }
}
