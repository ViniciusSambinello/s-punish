package com.spunish.paper.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

final class GuiItems {

    private GuiItems() {
    }

    static ItemStack of(String materialName, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(resolveMaterial(materialName));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static Material resolveMaterial(String name) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : Material.BARRIER;
    }
}
