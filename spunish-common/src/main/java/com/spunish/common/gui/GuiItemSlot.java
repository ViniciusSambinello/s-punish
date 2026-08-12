package com.spunish.common.gui;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * A single fixed inventory slot and the material key used as its icon. Titles,
 * item names and lore are text and therefore live in {@code messages.yml}
 * (see {@code MessageService}), resolved by convention from the menu/item name —
 * {@code gui.yml} only says where things sit and what icon they use.
 */
@ConfigSerializable
public record GuiItemSlot(int slot, String icon) {
}
