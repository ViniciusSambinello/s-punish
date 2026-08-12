package com.spunish.common.service;

import com.spunish.common.domain.Punishment;
import com.spunish.common.duration.PunishmentDuration;

/**
 * Every rejection reason from the {@code punishment/issuance} precedence
 * rules gets its own case so a caller can render the right configurable
 * message without string-matching a generic error.
 */
public sealed interface IssueResult {

    record Success(Punishment applied, Punishment revokedByOverride) implements IssueResult {
    }

    sealed interface Refused extends IssueResult {
    }

    record SelfPunishDenied() implements Refused {
    }

    record CategoryPermissionDenied() implements Refused {
    }

    record ReasonPermissionDenied() implements Refused {
    }

    record TargetExempt() implements Refused {
    }

    record AlreadyActive(Punishment existing) implements Refused {
    }

    record DurationLimitExceeded(PunishmentDuration limit) implements Refused {
    }

    record InternalError(Throwable cause) implements Refused {
    }
}
