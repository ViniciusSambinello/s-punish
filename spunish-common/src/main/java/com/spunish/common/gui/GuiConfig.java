package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Root of {@code gui.yml} — layout only, for the four Paper-side menus
 * (Velocity has no GUIs). See {@link GuiItemSlot} for why there is no text here.
 */
@ConfigSerializable
public record GuiConfig(
        @Setting("category-menu") CategoryMenuConfig categoryMenu,
        @Setting("reason-menu") ReasonMenuConfig reasonMenu,
        @Setting("history-menu") HistoryMenuConfig historyMenu,
        @Setting("report-menu") ReportMenuConfig reportMenu) {
}
