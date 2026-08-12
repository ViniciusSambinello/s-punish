package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Runtime, validated view of {@code reasons.yml}. Only {@link ReasonCatalogValidator}
 * constructs instances, so a {@code ReasonCatalog} in hand is always known-valid.
 */
public final class ReasonCatalog {

    private final Map<PunishmentCategory, List<Reason>> byCategory;

    ReasonCatalog(Map<PunishmentCategory, List<Reason>> byCategory) {
        this.byCategory = Map.copyOf(byCategory);
    }

    public List<Reason> forCategory(PunishmentCategory category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public Optional<Reason> find(PunishmentCategory category, String id) {
        return forCategory(category).stream()
                .filter(reason -> reason.id().equals(id))
                .findFirst();
    }

    /**
     * @param hasPermission checked against {@link Reason#permission()} for restricted
     *                       reasons; unrestricted reasons are always visible.
     */
    public List<Reason> visibleTo(PunishmentCategory category, Predicate<String> hasPermission) {
        return forCategory(category).stream()
                .filter(reason -> !reason.isRestricted() || hasPermission.test(reason.permission()))
                .toList();
    }
}
