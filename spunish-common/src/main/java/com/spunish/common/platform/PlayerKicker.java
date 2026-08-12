package com.spunish.common.platform;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface PlayerKicker {

    /**
     * No-op if the player is not currently reachable from this instance.
     */
    void kick(UUID uuid, Component reason);
}
