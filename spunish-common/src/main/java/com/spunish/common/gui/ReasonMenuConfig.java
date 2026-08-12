package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record ReasonMenuConfig(
        int size,
        @Setting("content-slots") List<Integer> contentSlots,
        @Setting("back-button") GuiItemSlot backButton,
        @Setting("previous-page") GuiItemSlot previousPage,
        @Setting("next-page") GuiItemSlot nextPage) {
}
