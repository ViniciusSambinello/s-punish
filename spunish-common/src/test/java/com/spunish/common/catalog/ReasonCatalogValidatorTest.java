package com.spunish.common.catalog;

import com.spunish.common.domain.PunishmentCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonCatalogValidatorTest {

    private final ReasonCatalogValidator validator = new ReasonCatalogValidator();

    private static ReasonDefinition reason(String id, String displayName, String duration) {
        return new ReasonDefinition(id, displayName, duration, "STONE", "desc", null);
    }

    @Test
    void validCatalogBuildsSuccessfully() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("hacking", "Hacking", "30d")),
                "mute", List.of(reason("spam", "Spam", "1h"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Success.class);
        ReasonCatalog catalog = ((CatalogLoadResult.Success) result).catalog();
        assertThat(catalog.find(PunishmentCategory.BAN, "hacking")).isPresent();
        assertThat(catalog.find(PunishmentCategory.MUTE, "spam")).isPresent();
    }

    @Test
    void sameIdAcrossDifferentCategoriesIsValid() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("spam", "Spam ban", "7d")),
                "mute", List.of(reason("spam", "Spam mute", "1h"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Success.class);
        ReasonCatalog catalog = ((CatalogLoadResult.Success) result).catalog();
        assertThat(catalog.find(PunishmentCategory.BAN, "spam")).isPresent();
        assertThat(catalog.find(PunishmentCategory.MUTE, "spam")).isPresent();
    }

    @Test
    void duplicateIdWithinSameCategoryFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("hacking", "Hacking", "30d"), reason("hacking", "Hacking 2", "7d"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
        assertThat(((CatalogLoadResult.Failure) result).errors())
                .anySatisfy(error -> assertThat(error.message()).contains("duplicate id"));
    }

    @Test
    void blankIdFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("ban", List.of(reason(" ", "Hacking", "30d"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
    }

    @Test
    void blankDisplayNameFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("ban", List.of(reason("hacking", "", "30d"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
    }

    @Test
    void missingDurationFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("ban", List.of(reason("hacking", "Hacking", null))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
    }

    @Test
    void unknownUnitFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("ban", List.of(reason("hacking", "Hacking", "7x"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
    }

    @Test
    void nonPositiveDurationFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("ban", List.of(reason("hacking", "Hacking", "0s"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
    }

    @Test
    void unknownCategoryFails() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of("kick", List.of(reason("rude", "Rude", "1h"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
        assertThat(((CatalogLoadResult.Failure) result).errors())
                .anySatisfy(error -> assertThat(error.message()).contains("unknown category"));
    }

    @Test
    void allProblemsAreReportedInOnePass() {
        ReasonCatalogFile file = new ReasonCatalogFile(Map.of(
                "ban", List.of(reason("", "Hacking", "30d"), reason("griefing", "", "7d"))));

        CatalogLoadResult result = validator.buildCatalog(file);

        assertThat(result).isInstanceOf(CatalogLoadResult.Failure.class);
        assertThat(((CatalogLoadResult.Failure) result).errors()).hasSize(2);
    }
}
