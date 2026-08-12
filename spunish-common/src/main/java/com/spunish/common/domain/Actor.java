package com.spunish.common.domain;

/**
 * Who performed an action against a punishment — applying it, or later revoking it.
 */
public sealed interface Actor permits PlayerActor, ConsoleActor, SystemActor {
}
