package com.spunish.common.domain;

public record ConsoleActor() implements Actor {

    public static final ConsoleActor INSTANCE = new ConsoleActor();
}
