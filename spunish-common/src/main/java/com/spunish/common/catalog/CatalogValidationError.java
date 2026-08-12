package com.spunish.common.catalog;

/**
 * @param path dotted/indexed location of the offending entry in {@code reasons.yml},
 *             e.g. {@code reasons.ban[2].duration}, for the log line the spec requires.
 */
public record CatalogValidationError(String path, String message) {

    @Override
    public String toString() {
        return path + ": " + message;
    }
}
