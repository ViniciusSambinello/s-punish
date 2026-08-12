package com.spunish.paper.gui;

import com.spunish.common.catalog.Reason;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.gui.ReasonMenuConfig;
import com.spunish.common.service.IssueCommand;
import com.spunish.paper.SPunishServices;
import com.spunish.paper.command.Actors;
import com.spunish.paper.command.IssueResultPresenter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.Map;

/**
 * Hides reasons the author lacks permission for. Applies the clicked
 * reason's own default duration; the GUI path never takes an explicit one.
 */
final class ReasonMenu extends Menu {

    private final SPunishServices services;
    private final PunishmentTarget target;
    private final PunishmentCategory category;
    private final boolean cameFromCategoryMenu;
    private final int page;
    private final List<Reason> visibleReasons;
    private final List<Integer> contentSlots;
    private final List<Reason> pageItems;
    private final IssueResultPresenter presenter;

    ReasonMenu(SPunishServices services, Player author, PunishmentTarget target, PunishmentCategory category,
            boolean cameFromCategoryMenu, int page) {
        super(GuiText.single(services.messageService(), "gui.reason.title", Map.of()),
                services.guiConfig().reasonMenu().size());
        this.services = services;
        this.target = target;
        this.category = category;
        this.cameFromCategoryMenu = cameFromCategoryMenu;
        this.page = page;
        this.presenter = new IssueResultPresenter(services);
        this.visibleReasons = services.catalogHolder().current()
                .visibleTo(category, permission -> services.permissionChecker().hasPermission(Actors.of(author), permission));
        this.contentSlots = services.guiConfig().reasonMenu().contentSlots();

        int from = Math.min(page * contentSlots.size(), visibleReasons.size());
        int to = Math.min(from + contentSlots.size(), visibleReasons.size());
        this.pageItems = visibleReasons.subList(from, to);
        render();
    }

    private void render() {
        ReasonMenuConfig config = services.guiConfig().reasonMenu();
        for (int i = 0; i < contentSlots.size(); i++) {
            int slot = contentSlots.get(i);
            if (i >= pageItems.size()) {
                clear(slot);
                continue;
            }
            Reason reason = pageItems.get(i);
            Map<String, String> placeholders = Map.of(
                    "reason-display", reason.displayName(),
                    "duration", services.messageService().formatDuration(reason.duration()));
            setItem(slot, GuiItems.of(
                    reason.icon(),
                    GuiText.single(services.messageService(), "gui.reason.item.name", placeholders),
                    services.messageService().render("gui.reason.item.lore", placeholders)));
        }

        if (cameFromCategoryMenu) {
            setItem(config.backButton().slot(), GuiItems.of(
                    config.backButton().icon(), GuiText.single(services.messageService(), "gui.reason.back", Map.of()), List.of()));
        }
        boolean hasPrevious = page > 0;
        boolean hasNext = page * contentSlots.size() + contentSlots.size() < visibleReasons.size();
        if (hasPrevious) {
            setItem(config.previousPage().slot(), GuiItems.of(config.previousPage().icon(),
                    GuiText.single(services.messageService(), "gui.reason.previous-page", Map.of()), List.of()));
        }
        if (hasNext) {
            setItem(config.nextPage().slot(), GuiItems.of(config.nextPage().icon(),
                    GuiText.single(services.messageService(), "gui.reason.next-page", Map.of()), List.of()));
        }
    }

    @Override
    void onClick(Player player, int slot, ClickType clickType) {
        ReasonMenuConfig config = services.guiConfig().reasonMenu();
        if (cameFromCategoryMenu && slot == config.backButton().slot()) {
            new CategoryMenu(services, player, target).open(player);
            return;
        }
        if (page > 0 && slot == config.previousPage().slot()) {
            new ReasonMenu(services, player, target, category, cameFromCategoryMenu, page - 1).open(player);
            return;
        }
        if (page * contentSlots.size() + contentSlots.size() < visibleReasons.size() && slot == config.nextPage().slot()) {
            new ReasonMenu(services, player, target, category, cameFromCategoryMenu, page + 1).open(player);
            return;
        }

        int index = contentSlots.indexOf(slot);
        if (index < 0 || index >= pageItems.size()) {
            return;
        }
        apply(player, pageItems.get(index));
    }

    private void apply(Player player, Reason reason) {
        IssueCommand command = new IssueCommand(target, Actors.of(player), category, reason, null);
        services.issueService().issue(command).thenAccept(result ->
                services.mainThreadDispatcher().runOnMainThread(() -> {
                    presenter.present(player, command, result);
                    player.closeInventory();
                }));
    }
}
