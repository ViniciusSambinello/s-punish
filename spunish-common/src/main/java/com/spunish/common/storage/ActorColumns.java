package com.spunish.common.storage;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.ConsoleActor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.SystemActor;

/**
 * Maps {@link Actor} to and from the {@code (*_type, *_uuid, *_name)} column
 * triples shared by {@code actor_*} and {@code revoker_*}.
 */
final class ActorColumns {

    private ActorColumns() {
    }

    record Mapped(String type, byte[] uuid, String name) {
    }

    static Mapped from(Actor actor) {
        return switch (actor) {
            case PlayerActor player -> new Mapped("PLAYER", UuidBinary.toBytes(player.uuid()), player.name());
            case ConsoleActor console -> new Mapped("CONSOLE", null, "CONSOLE");
            case SystemActor system -> new Mapped("SYSTEM", null, "SYSTEM");
        };
    }

    static Actor toActor(String type, byte[] uuidBytes, String name) {
        return switch (type) {
            case "PLAYER" -> new PlayerActor(UuidBinary.fromBytes(uuidBytes), name);
            case "CONSOLE" -> ConsoleActor.INSTANCE;
            case "SYSTEM" -> SystemActor.INSTANCE;
            default -> throw new IllegalArgumentException("Unknown actor type in database row: " + type);
        };
    }
}
