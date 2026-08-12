package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record ReportMenuConfig(
        int size,
        @Setting("summary-slot") int summarySlot,
        @Setting("window-daily") GuiItemSlot windowDaily,
        @Setting("window-weekly") GuiItemSlot windowWeekly,
        @Setting("window-monthly") GuiItemSlot windowMonthly,
        @Setting("window-all-time") GuiItemSlot windowAllTime,
        @Setting("ranking-slots") List<Integer> rankingSlots,
        @Setting("reason-slots") List<Integer> reasonSlots) {
}
