package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record GuiConfig(
        @Setting("category-menu") CategoryMenuConfig categoryMenu,
        @Setting("reason-menu") ReasonMenuConfig reasonMenu,
        @Setting("history-menu") HistoryMenuConfig historyMenu,
        @Setting("report-menu") ReportMenuConfig reportMenu) {
}
