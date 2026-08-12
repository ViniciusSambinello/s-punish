package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record MuteSettings(
        @Setting("blocked-commands") List<String> blockedCommands,
        @Setting("warning-cooldown-ms") long warningCooldownMs) {
}
