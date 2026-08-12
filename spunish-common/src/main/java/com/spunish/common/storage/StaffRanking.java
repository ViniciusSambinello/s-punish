package com.spunish.common.storage;

import java.util.UUID;

/**
 * {@code staffName} is the most recently applied punishment's {@code actor_name}
 * for this staffer in the aggregated window — a punishment's own actor name is
 * frozen at the time it was applied, so this does not necessarily reflect the
 * staffer's current name.
 */
public record StaffRanking(UUID staffUuid, String staffName, long count) {
}
