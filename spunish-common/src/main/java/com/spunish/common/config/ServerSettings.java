package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * {@code id} blank or absent means the server must derive its own identity.
 */
@ConfigSerializable
public record ServerSettings(String id) {
}
