package com.spunish.common.domain;

public sealed interface Actor permits PlayerActor, ConsoleActor, SystemActor {
}
