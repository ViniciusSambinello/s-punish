package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.duration.DurationParser;
import com.spunish.common.duration.PunishmentDuration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates {@code reasons.yml} in full before building the runtime
 * {@link ReasonCatalog}: every entry is checked, so a single reload reports
 * every problem at once rather than stopping at the first one.
 */
public final class ReasonCatalogValidator {

    private final DurationParser durationParser = new DurationParser();

    public CatalogLoadResult buildCatalog(ReasonCatalogFile file) {
        List<CatalogValidationError> errors = new ArrayList<>();
        Map<PunishmentCategory, List<Reason>> byCategory = new EnumMap<>(PunishmentCategory.class);

        for (Map.Entry<String, List<ReasonDefinition>> entry : file.reasons().entrySet()) {
            String categoryKey = entry.getKey();
            Optional<PunishmentCategory> category = PunishmentCategory.fromString(categoryKey);
            if (category.isEmpty()) {
                errors.add(new CatalogValidationError("reasons." + categoryKey, "unknown category: " + categoryKey));
                continue;
            }
            byCategory.put(category.get(), validateCategory(categoryKey, category.get(), entry.getValue(), errors));
        }

        if (!errors.isEmpty()) {
            return new CatalogLoadResult.Failure(List.copyOf(errors));
        }
        return new CatalogLoadResult.Success(new ReasonCatalog(byCategory));
    }

    private List<Reason> validateCategory(
            String categoryKey,
            PunishmentCategory category,
            List<ReasonDefinition> definitions,
            List<CatalogValidationError> errors) {
        List<Reason> reasons = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        List<ReasonDefinition> safeDefinitions = definitions == null ? List.of() : definitions;

        for (int index = 0; index < safeDefinitions.size(); index++) {
            ReasonDefinition definition = safeDefinitions.get(index);
            String path = "reasons." + categoryKey + "[" + index + "]";

            if (isBlank(definition.id())) {
                errors.add(new CatalogValidationError(path + ".id", "id is missing or blank"));
                continue;
            }
            if (!seenIds.add(definition.id())) {
                errors.add(new CatalogValidationError(path + ".id",
                        "duplicate id '" + definition.id() + "' in category " + categoryKey));
                continue;
            }
            if (isBlank(definition.displayName())) {
                errors.add(new CatalogValidationError(path + ".display-name", "display-name is missing or blank"));
                continue;
            }
            if (isBlank(definition.duration())) {
                errors.add(new CatalogValidationError(path + ".duration", "duration is missing"));
                continue;
            }

            Optional<PunishmentDuration> parsedDuration = durationParser.parse(definition.duration());
            if (parsedDuration.isEmpty()) {
                errors.add(new CatalogValidationError(path + ".duration",
                        "invalid duration '" + definition.duration()
                                + "' (use 'n' for permanent, or a quantity+unit sequence: s, m, h, d, w, mo, y)"));
                continue;
            }

            reasons.add(new Reason(
                    definition.id(),
                    category,
                    definition.displayName(),
                    parsedDuration.get(),
                    definition.icon(),
                    definition.description(),
                    definition.permission()));
        }

        return reasons;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
