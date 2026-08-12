package com.spunish.common.storage;

import com.spunish.common.domain.Punishment;

public record OverrideResult(Punishment applied, Punishment revoked) {
}
