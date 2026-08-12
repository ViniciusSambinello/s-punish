package com.spunish.common.platform;

import net.kyori.adventure.audience.Audience;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AudienceResolver {

    Optional<Audience> online(UUID uuid);

    Collection<Audience> onlineWithPermission(String permission);

    /**
     * Everyone online here — for public, disable-able announcements.
     */
    Audience broadcastAudience();
}
