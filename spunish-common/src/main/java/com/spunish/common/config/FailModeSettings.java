package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record FailModeSettings(FailMode login, FailMode chat) {
}
