package com.spunish.paper.gui;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.gui.HistoryMenuConfig;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.HistoryEntry;
import com.spunish.paper.SPunishServices;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Filtering resets to the first page (history spec) and every page is
 * fetched on demand — nothing beyond the visible page is ever loaded.
 */
final class HistoryMenu extends Menu {

    private final SPunishServices services;
    private final PunishmentTarget target;
    private final List<Integer> contentSlots;
    private Optional<PunishmentCategory> filter;
    private int page;
    private List<HistoryEntry> entries = List.of();
    private boolean hasNext;

    HistoryMenu(SPunishServices services, Player viewer, PunishmentTarget target, Optional<PunishmentCategory> filter, int page) {
        super(GuiText.single(services.messageService(), "gui.history.title", Map.of("target", target.name())),
                services.guiConfig().historyMenu().size());
        this.services = services;
        this.target = target;
        this.contentSlots = services.guiConfig().historyMenu().contentSlots();
        this.filter = filter;
        this.page = page;
        renderControls();
        load(viewer);
    }

    private void load(Player viewer) {
        services.historyService().page(target.uuid(), filter, page, contentSlots.size())
                .thenAccept(result -> services.mainThreadDispatcher().runOnMainThread(() -> {
                    this.entries = result.entries();
                    this.hasNext = result.hasNext();
                    renderContent();
                }))
                .exceptionally(ex -> {
                    services.logger().log(Level.WARNING, "History lookup failed for " + target.name(), ex);
                    services.mainThreadDispatcher().runOnMainThread(viewer::closeInventory);
                    return null;
                });
    }

    private void renderControls() {
        HistoryMenuConfig config = services.guiConfig().historyMenu();
        setItem(config.filterAll().slot(), GuiItems.of(config.filterAll().icon(),
                GuiText.single(services.messageService(), "gui.history.filter-all", Map.of()), List.of()));
        setItem(config.filterBan().slot(), GuiItems.of(config.filterBan().icon(),
                GuiText.single(services.messageService(), "gui.history.filter-ban", Map.of()), List.of()));
        setItem(config.filterMute().slot(), GuiItems.of(config.filterMute().icon(),
                GuiText.single(services.messageService(), "gui.history.filter-mute", Map.of()), List.of()));
    }

    private void renderContent() {
        HistoryMenuConfig config = services.guiConfig().historyMenu();
        Instant now = services.clock().now();
        for (int i = 0; i < contentSlots.size(); i++) {
            int slot = contentSlots.get(i);
            if (i >= entries.size()) {
                clear(slot);
                continue;
            }
            HistoryEntry entry = entries.get(i);
            Map<String, String> placeholders = new HashMap<>(
                    PunishmentPlaceholders.build(entry.punishment(), services.messageService(), now));
            placeholders.put("state", services.messageService().stateLabelText(entry.state()));
            String icon = services.guiConfig().categoryMenu().items().get(entry.punishment().category().permissionSuffix()).icon();
            setItem(slot, GuiItems.of(icon,
                    GuiText.single(services.messageService(), "gui.history.entry.name", placeholders),
                    services.messageService().render("gui.history.entry.lore", placeholders)));
        }
        if (entries.isEmpty() && page == 0) {
            setItem(contentSlots.get(contentSlots.size() / 2), GuiItems.of("PAPER",
                    GuiText.single(services.messageService(), "history.empty", Map.of("target", target.name())), List.of()));
        }

        if (page > 0) {
            setItem(config.previousPage().slot(), GuiItems.of(config.previousPage().icon(),
                    GuiText.single(services.messageService(), "gui.history.previous-page", Map.of()), List.of()));
        } else {
            clear(config.previousPage().slot());
        }
        if (hasNext) {
            setItem(config.nextPage().slot(), GuiItems.of(config.nextPage().icon(),
                    GuiText.single(services.messageService(), "gui.history.next-page", Map.of()), List.of()));
        } else {
            clear(config.nextPage().slot());
        }
    }

    @Override
    void onClick(Player player, int slot, ClickType clickType) {
        HistoryMenuConfig config = services.guiConfig().historyMenu();
        if (slot == config.filterAll().slot()) {
            setFilter(player, Optional.empty());
            return;
        }
        if (slot == config.filterBan().slot()) {
            setFilter(player, Optional.of(PunishmentCategory.BAN));
            return;
        }
        if (slot == config.filterMute().slot()) {
            setFilter(player, Optional.of(PunishmentCategory.MUTE));
            return;
        }
        if (page > 0 && slot == config.previousPage().slot()) {
            page--;
            load(player);
            return;
        }
        if (hasNext && slot == config.nextPage().slot()) {
            page++;
            load(player);
            return;
        }

        int index = contentSlots.indexOf(slot);
        if (index < 0 || index >= entries.size()) {
            return;
        }
        new HistoryDetailMenu(services, entries.get(index), this).open(player);
    }

    private void setFilter(Player player, Optional<PunishmentCategory> newFilter) {
        this.filter = newFilter;
        this.page = 0;
        load(player);
    }
}
