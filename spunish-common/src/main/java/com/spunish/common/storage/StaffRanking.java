package com.spunish.common.storage;

import java.util.UUID;

/**
 * No display name here on purpose — a punishment's {@code actor_name} is
 * frozen at the time it was applied, so resolving a staffer's *current* name
 * for a ranking is the caller's job (via the profiles table), not this query's.
 */
public record StaffRanking(UUID staffUuid, long count) {
}
