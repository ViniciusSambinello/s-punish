package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
public record CategoryMenuConfig(int size, Map<String, GuiItemSlot> items) {
}
