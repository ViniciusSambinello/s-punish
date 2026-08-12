package com.spunish.paper;

/**
 * The plugin refuses to enable when the database is unreachable, the schema
 * is newer than this binary supports, or {@code reasons.yml} fails validation.
 */
public final class SPunishBootstrapException extends Exception {

    private static final long serialVersionUID = 1L;

    public SPunishBootstrapException(String message) {
        super(message);
    }

    public SPunishBootstrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
