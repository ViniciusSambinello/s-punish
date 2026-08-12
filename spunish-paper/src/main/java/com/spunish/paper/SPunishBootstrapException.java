package com.spunish.paper;

/**
 * Thrown by {@link SPunishServices#bootstrap} for any condition that must
 * refuse plugin enablement outright: an unreachable database, a schema
 * newer than this binary supports, or an invalid {@code reasons.yml}
 * (catalog spec's "recusa-se a iniciar quando a configuração for inválida").
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
