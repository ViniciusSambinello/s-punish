package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
     * @param hasPermission restricted reasons are shown only if this check passes;
     *                       unrestricted reasons are always visible.
     */
    public List<Reason> visibleTo(PunishmentCategory category, Predicate<String> hasPermission) {
        return forCategory(category).stream()
                .filter(reason -> !reason.isRestricted() || hasPermission.test(reason.permission()))
                .toList();
    }
}
