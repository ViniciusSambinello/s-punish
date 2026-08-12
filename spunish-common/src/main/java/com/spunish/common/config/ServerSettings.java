package com.spunish.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * {@code id} blank/absent means "derive one" — see {@code punishment/network-sync}'s
 * server identity requirement.
 */
@ConfigSerializable
public record ServerSettings(String id) {
}
