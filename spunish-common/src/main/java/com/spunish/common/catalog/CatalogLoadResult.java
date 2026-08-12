package com.spunish.common.catalog;

import java.util.List;

public sealed interface CatalogLoadResult permits CatalogLoadResult.Success, CatalogLoadResult.Failure {

    record Success(ReasonCatalog catalog) implements CatalogLoadResult {
    }

    record Failure(List<CatalogValidationError> errors) implements CatalogLoadResult {
    }
}
