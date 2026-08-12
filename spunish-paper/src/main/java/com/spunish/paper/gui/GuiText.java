package com.spunish.paper.gui;

import com.spunish.common.message.MessageService;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

final class GuiText {

    private GuiText() {
    }

    static Component single(MessageService messages, String key, Map<String, String> placeholders) {
        List<Component> lines = messages.render(key, placeholders);
        return lines.isEmpty() ? Component.empty() : lines.get(0);
    }
}
