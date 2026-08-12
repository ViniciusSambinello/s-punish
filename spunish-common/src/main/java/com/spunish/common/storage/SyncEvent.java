package com.spunish.common.storage;

import java.time.Instant;
import java.util.UUID;

public record SyncEvent(
        long id, SyncEventType type, long punishmentId, UUID targetUuid, String originServer, Instant createdAt) {
}
