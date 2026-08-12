package com.spunish.common.storage;

import com.spunish.common.domain.PlayerProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileRepository {

    CompletableFuture<Void> upsert(PlayerProfile profile);

    CompletableFuture<Optional<PlayerProfile>> findByUuid(UUID uuid);

    /**
     * When a name was used by more than one account over time, the profile
     * with the most recent {@code last_seen_at} wins.
     */
    CompletableFuture<Optional<PlayerProfile>> findByName(String name);
}
