package com.spunish.common.gui;

import com.spunish.common.config.YamlConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GuiConfigLoaderTest {

    @Test
    void loadsBundledDefaultGuiIntoRecord(@TempDir Path tempDir) {
        Path target = tempDir.resolve("gui.yml");

        GuiConfig gui = YamlConfigLoader.load(target, "/default/gui.yml", GuiConfig.class);

        assertThat(target).exists();
        assertThat(gui.categoryMenu().size()).isEqualTo(27);
        assertThat(gui.categoryMenu().items().get("ban").slot()).isEqualTo(11);
        assertThat(gui.reasonMenu().contentSlots()).contains(10, 34);
        assertThat(gui.historyMenu().filterBan().icon()).isEqualTo("RED_STAINED_GLASS_PANE");
        assertThat(gui.reportMenu().rankingSlots()).hasSize(7);
    }
}
