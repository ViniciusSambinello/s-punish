package com.spunish.paper.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

final class GuiItems {

    private GuiItems() {
    }

    /**
     * These are menu buttons, not real equipment — any vanilla tooltip
     * addition (attack damage/speed for a sword icon, armor toughness for an
     * armor icon, "can be placed on", etc.) would be noise the player didn't
     * configure, so every such addition is hidden regardless of icon material.
     */
    static ItemStack of(String materialName, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(resolveMaterial(materialName));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private static Material resolveMaterial(String name) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : Material.BARRIER;
    }
}
