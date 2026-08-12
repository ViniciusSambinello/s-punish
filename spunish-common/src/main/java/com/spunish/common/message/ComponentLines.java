package com.spunish.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.List;

/**
 * Joins a rendered message's lines into one {@link Component} (newline-separated)
 * for APIs that take a single kick/disallow reason rather than multiple lines.
 */
public final class ComponentLines {

    private ComponentLines() {
    }

    public static Component join(List<Component> lines) {
        return lines.isEmpty() ? Component.empty() : Component.join(JoinConfiguration.separator(Component.newline()), lines);
    }
}
