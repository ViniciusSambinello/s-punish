package com.spunish.common.service;

import com.spunish.common.domain.Punishment;
import com.spunish.common.duration.PunishmentDuration;

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
