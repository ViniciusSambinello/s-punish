package com.spunish.common.catalog;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record ReasonDefinition(
        String id,
        @Setting("display-name") String displayName,
        String duration,
        String icon,
        String description,
        String permission) {
}
