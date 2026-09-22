package com.spunish.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A malformed MiniMessage tag degrades to plain (unformatted) text rather
 * than failing the send; the broken key is logged once.
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

    /**
     * Escapes any MiniMessage tag syntax in {@code text} so it renders as
     * literal characters instead of being parsed as formatting/click/hover
     * tags. Used on untrusted (player- or staff-supplied) placeholder values
     * before they are spliced into an admin-authored message template, so a
     * crafted value (for example a player name obtained on an offline-mode
     * server) can never smuggle a {@code <click:run_command:...>} or similar
     * component into text another user later views or clicks.
     */
    public String escapeTags(String text) {
        return miniMessage.escapeTags(text);
    }
}
