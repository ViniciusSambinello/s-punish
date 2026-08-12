package com.spunish.velocity;

/**
 * The proxy module refuses to enable when the database is unreachable or
 * {@code config.yml} fails validation — the same refuse-to-start rule the
 * backend plugin follows.
 */
public final class SPunishVelocityBootstrapException extends Exception {

    private static final long serialVersionUID = 1L;

    public SPunishVelocityBootstrapException(String message) {
        super(message);
    }

    public SPunishVelocityBootstrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
