package com.spunish.common.catalog;

public record CatalogValidationError(String path, String message) {

    @Override
    public String toString() {
        return path + ": " + message;
    }
}
