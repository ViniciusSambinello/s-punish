package com.spunish.common.catalog;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * One entry of {@code reasons.yml}. {@code duration} is kept as raw text here
 * ({@code "n"} or a {@code DurationParser}-compatible sequence like
 * {@code "30d"}) — parsing happens once, at catalog validation time, not on
 * every read.
 */
@ConfigSerializable
public record ReasonDefinition(
        String id,
        @Setting("display-name") String displayName,
        String duration,
        String icon,
        String description,
        String permission) {
}
