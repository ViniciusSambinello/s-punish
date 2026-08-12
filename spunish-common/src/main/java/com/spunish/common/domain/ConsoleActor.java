package com.spunish.common.domain;

/**
 * Marker actor for commands executed by the server console.
 */
public record ConsoleActor() implements Actor {

    public static final ConsoleActor INSTANCE = new ConsoleActor();
}
