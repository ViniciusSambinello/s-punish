package com.spunish.common.service;

import com.spunish.common.domain.Punishment;

public sealed interface RevokeResult {

    record Success(Punishment revoked) implements RevokeResult {
    }

    sealed interface Refused extends RevokeResult {
    }

    record NoActivePunishment() implements Refused {
    }

    record PermissionDenied() implements Refused {
    }

    record InternalError(Throwable cause) implements Refused {
    }
}
