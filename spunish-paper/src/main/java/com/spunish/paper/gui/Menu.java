package com.spunish.paper.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Closing a menu without clicking a selection never creates or changes any record.
 */
public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;

    protected Menu(Component title, int size) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    public final void open(Player player) {
        player.openInventory(inventory);
    }

    protected final void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected final void clear(int slot) {
        inventory.setItem(slot, null);
    }

    abstract void onClick(Player player, int slot, ClickType clickType);

    void onClose(Player player) {
    }
}
