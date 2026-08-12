package com.spunish.common.storage;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts to/from the {@code BINARY(16)} representation used for every UUID
 * column — half the index footprint of a {@code CHAR(36)}.
 */
final class UuidBinary {

    private UuidBinary() {
    }

    static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    static UUID fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
