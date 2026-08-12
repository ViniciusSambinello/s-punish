package com.spunish.common.platform;

import net.kyori.adventure.audience.Audience;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves *locally connected* players only — a staff announcement reaching
 * the whole network is achieved by every instance separately notifying its
 * own local audience (once for a local application, once more wherever the
 * resulting sync event is consumed), not by this port reaching across servers.
 */
public interface AudienceResolver {

    Optional<Audience> online(UUID uuid);

    Collection<Audience> onlineWithPermission(String permission);

    /**
     * Everyone online here — for public, disable-able announcements.
     */
    Audience broadcastAudience();
}
