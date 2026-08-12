package com.spunish.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Renders MiniMessage text, degrading to plain (unformatted) text on a
 * malformed tag instead of failing the send — the broken key is logged once.
 * Uses a strict-mode instance: the default non-strict {@code MiniMessage.miniMessage()}
 * silently renders unclosed/unresolvable tags as literal text and never
 * throws, which would make this degrade-and-log path unreachable.
 */
public final class MiniMessageRenderer {

    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();
    private final BiConsumer<String, ParsingException> onMalformedTag;

    public MiniMessageRenderer(BiConsumer<String, ParsingException> onMalformedTag) {
        this.onMalformedTag = onMalformedTag;
    }

    public Component render(String key, String text) {
        try {
            return miniMessage.deserialize(text);
        } catch (ParsingException malformed) {
            if (warnedKeys.add(key)) {
                onMalformedTag.accept(key, malformed);
            }
            return Component.text(text);
        }
    }
}
