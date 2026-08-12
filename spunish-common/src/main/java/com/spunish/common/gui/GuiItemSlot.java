package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record GuiItemSlot(int slot, String icon) {
}
