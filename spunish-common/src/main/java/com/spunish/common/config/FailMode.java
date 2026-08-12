package com.spunish.common.config;

/**
 * What to do when punishment state cannot be determined because storage is
 * unavailable. Defaults differ by call site: {@code DENY} for login,
 * {@code ALLOW} for chat (see {@code punishment/enforcement}).
 */
public enum FailMode {
    ALLOW,
    DENY
}
