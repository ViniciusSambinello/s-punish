package com.spunish.common.storage;

import java.security.SecureRandom;

/**
 * Generates the 8-character identifier shown on rejection screens and used
 * by staff to locate a record. Excludes visually ambiguous characters
 * (0/O, 1/I/L) since it is read off a screen and typed back by hand.
 */
final class PublicIdGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
