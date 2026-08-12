package com.spunish.paper.gui;

import com.spunish.common.domain.PunishmentState;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.HistoryEntry;
import com.spunish.paper.SPunishServices;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full detail of one punishment, including its identifier and origin server.
 */
final class HistoryDetailMenu extends Menu {

    private static final int BACK_SLOT = 0;
    private static final int DETAIL_SLOT = 4;

    private final HistoryMenu parent;

    HistoryDetailMenu(SPunishServices services, HistoryEntry entry, HistoryMenu parent) {
        super(GuiText.single(services.messageService(), "gui.history.detail.title", Map.of()), 9);
        this.parent = parent;
        render(services, entry);
    }

    private void render(SPunishServices services, HistoryEntry entry) {
        Instant now = services.clock().now();
        Map<String, String> placeholders = new HashMap<>(
                PunishmentPlaceholders.build(entry.punishment(), services.messageService(), now));
        placeholders.put("state", services.messageService().stateLabelText(entry.state()));

        List<Component> lore = new ArrayList<>(services.messageService().render("gui.history.entry.lore", placeholders));
        lore.addAll(services.messageService().render("gui.history.detail.lore", placeholders));
        if (entry.state() instanceof PunishmentState.Revoked) {
            lore.addAll(services.messageService().render("gui.history.detail.revocation-lore", placeholders));
        }

        String icon = services.guiConfig().categoryMenu().items().get(entry.punishment().category().permissionSuffix()).icon();
        setItem(DETAIL_SLOT, GuiItems.of(icon,
                GuiText.single(services.messageService(), "gui.history.entry.name", placeholders), lore));
        setItem(BACK_SLOT, GuiItems.of(services.guiConfig().reasonMenu().backButton().icon(),
                GuiText.single(services.messageService(), "gui.reason.back", Map.of()), List.of()));
    }

    @Override
    void onClick(Player player, int slot, ClickType clickType) {
        if (slot == BACK_SLOT) {
            parent.open(player);
        }
    }
}
