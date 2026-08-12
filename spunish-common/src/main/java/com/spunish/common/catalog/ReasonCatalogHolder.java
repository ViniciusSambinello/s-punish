package com.spunish.common.catalog;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the currently effective {@link ReasonCatalog} and swaps it atomically
 * on reload: the replacement candidate is built and validated in full before
 * anything about the current reference changes, so an invalid edit to
 * {@code reasons.yml} never leaves the catalog partially replaced.
 */
public final class ReasonCatalogHolder {

    private final ReasonCatalogValidator validator = new ReasonCatalogValidator();
    private final AtomicReference<ReasonCatalog> current;

    public ReasonCatalogHolder(ReasonCatalog initial) {
        this.current = new AtomicReference<>(Objects.requireNonNull(initial, "initial"));
    }

    public ReasonCatalog current() {
        return current.get();
    }

    public CatalogLoadResult reload(ReasonCatalogFile file) {
        CatalogLoadResult result = validator.buildCatalog(file);
        if (result instanceof CatalogLoadResult.Success success) {
            current.set(success.catalog());
        }
        return result;
    }
}
