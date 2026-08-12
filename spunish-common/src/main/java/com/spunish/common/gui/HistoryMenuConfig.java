package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record HistoryMenuConfig(
        int size,
        @Setting("content-slots") List<Integer> contentSlots,
        @Setting("previous-page") GuiItemSlot previousPage,
        @Setting("next-page") GuiItemSlot nextPage,
        @Setting("filter-all") GuiItemSlot filterAll,
        @Setting("filter-ban") GuiItemSlot filterBan,
        @Setting("filter-mute") GuiItemSlot filterMute) {
}
