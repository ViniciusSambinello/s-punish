package com.spunish.common.catalog;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;
import java.util.Map;

@ConfigSerializable
public record ReasonCatalogFile(Map<String, List<ReasonDefinition>> reasons) {
}
