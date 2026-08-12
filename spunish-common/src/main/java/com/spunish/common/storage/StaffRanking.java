package com.spunish.common.storage;

import java.util.UUID;

/**
 * A punishment's {@code actor_name} is frozen at the time it was applied and
 * does not reflect the staffer's current name.
 */
public record StaffRanking(UUID staffUuid, long count) {
}
