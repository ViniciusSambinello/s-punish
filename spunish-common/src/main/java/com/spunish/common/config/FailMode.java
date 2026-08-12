package com.spunish.common.config;

/**
 * What to do when punishment state cannot be determined because storage is
 * unavailable. Login defaults to {@code DENY}; chat defaults to
 * {@code ALLOW}.
 */
public enum FailMode {
    ALLOW,
    DENY
}
