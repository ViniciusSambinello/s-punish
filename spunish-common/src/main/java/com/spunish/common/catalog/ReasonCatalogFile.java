package com.spunish.common.catalog;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;
import java.util.Map;

/**
 * Raw shape of {@code reasons.yml}: a list of reasons per category name, as
 * written in the file (not yet validated against the known
 * {@link com.spunish.common.domain.PunishmentCategory} values).
 */
@ConfigSerializable
public record ReasonCatalogFile(Map<String, List<ReasonDefinition>> reasons) {
}
