package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonCatalogHolderTest {

    private final ReasonCatalogValidator validator = new ReasonCatalogValidator();

    private static ReasonDefinition reason(String id, String displayName, String duration) {
        return new ReasonDefinition(id, displayName, duration, "STONE", "desc", null);
    }

    @Test
    void invalidReloadPreservesPreviousCatalogIntact() {
        ReasonCatalogFile validFile = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("hacking", "Hacking", "30d"))));
        ReasonCatalog initialCatalog = ((CatalogLoadResult.Success) validator.buildCatalog(validFile)).catalog();
        ReasonCatalogHolder holder = new ReasonCatalogHolder(initialCatalog);

        ReasonCatalogFile brokenFile = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("hacking", "Hacking", "30d"), reason("hacking", "Duplicate", "1d"))));

        CatalogLoadResult result = holder.reload(brokenFile);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
        assertThat(holder.current()).isSameAs(initialCatalog);
        assertThat(holder.current().find(PunishmentCategory.BAN, "hacking")).isPresent();
    }

    @Test
    void validReloadReplacesCatalog() {
        ReasonCatalogFile initialFile = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("hacking", "Hacking", "30d"))));
        ReasonCatalog initialCatalog = ((CatalogLoadResult.Success) validator.buildCatalog(initialFile)).catalog();
        ReasonCatalogHolder holder = new ReasonCatalogHolder(initialCatalog);

        ReasonCatalogFile newFile = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("griefing", "Griefing", "7d"))));

        CatalogLoadResult result = holder.reload(newFile);

        assertThat(result).isInstanceOf(CatalogLoadResult.Success.class);
        assertThat(holder.current()).isNotSameAs(initialCatalog);
        assertThat(holder.current().find(PunishmentCategory.BAN, "griefing")).isPresent();
        assertThat(holder.current().find(PunishmentCategory.BAN, "hacking")).isEmpty();
    }
}
