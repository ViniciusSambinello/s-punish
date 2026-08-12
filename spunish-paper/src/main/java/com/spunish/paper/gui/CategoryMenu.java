package com.spunish.paper.gui;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.gui.CategoryMenuConfig;
import com.spunish.common.gui.GuiItemSlot;
import com.spunish.paper.SPunishServices;
import com.spunish.paper.command.Actors;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Map;

/**
 * Shows only the categories the author may apply (issuance spec) — a
 * category the author lacks permission for never appears here.
 */
final class CategoryMenu extends Menu {

    private final SPunishServices services;
    private final PunishmentTarget target;

    CategoryMenu(SPunishServices services, Player author, PunishmentTarget target) {
        super(GuiText.single(services.messageService(), "gui.category.title", Map.of()),
                services.guiConfig().categoryMenu().size());
        this.services = services;
        this.target = target;
        render(author);
    }

    private void render(Player author) {
        CategoryMenuConfig config = services.guiConfig().categoryMenu();
        Map<String, String> placeholders = Map.of("target", target.name());
        for (PunishmentCategory category : PunishmentCategory.values()) {
            String key = category.permissionSuffix();
            GuiItemSlot slot = config.items().get(key);
            if (slot == null || !services.permissionChecker().hasPermission(Actors.of(author), "spunish.punish." + key)) {
                continue;
            }
            setItem(slot.slot(), GuiItems.of(
                    slot.icon(),
                    GuiText.single(services.messageService(), "gui.category.item." + key + ".name", placeholders),
                    services.messageService().render("gui.category.item." + key + ".lore", placeholders)));
        }
    }

    @Override
    void onClick(Player player, int slot, ClickType clickType) {
        CategoryMenuConfig config = services.guiConfig().categoryMenu();
        for (Map.Entry<String, GuiItemSlot> entry : config.items().entrySet()) {
            if (entry.getValue().slot() != slot) {
                continue;
            }
            PunishmentCategory.fromString(entry.getKey()).ifPresent(category -> {
                if (services.permissionChecker().hasPermission(Actors.of(player), "spunish.punish." + entry.getKey())) {
                    new ReasonMenu(services, player, target, category, true, 0).open(player);
                }
            });
            return;
        }
    }
}
