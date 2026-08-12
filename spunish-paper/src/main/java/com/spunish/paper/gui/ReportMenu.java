package com.spunish.paper.gui;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.gui.GuiItemSlot;
import com.spunish.common.gui.ReportMenuConfig;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.service.GeneralReportSummary;
import com.spunish.common.service.ReportOutcome;
import com.spunish.common.service.ReportWindow;
import com.spunish.common.service.StafferReportSummary;
import com.spunish.common.storage.ReasonDistributionEntry;
import com.spunish.common.storage.StaffRanking;
import com.spunish.common.storage.StateBreakdown;
import com.spunish.paper.SPunishServices;
import com.spunish.paper.command.CommandSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * {@code staffer == null} is the staff-wide report; otherwise the report is
 * restricted to that staffer's own punishments.
 */
final class ReportMenu extends Menu {

    private final SPunishServices services;
    private final PunishmentCategory category;
    private final PunishmentTarget staffer;
    private ReportWindow window = ReportWindow.DAILY;
    private boolean loadedOnce;

    ReportMenu(SPunishServices services, Player viewer, PunishmentCategory category, PunishmentTarget staffer) {
        super(title(services, category, staffer), services.guiConfig().reportMenu().size());
        this.services = services;
        this.category = category;
        this.staffer = staffer;
        renderWindowButtons();
        load(viewer);
    }

    private static Component title(SPunishServices services, PunishmentCategory category, PunishmentTarget staffer) {
        Map<String, String> placeholders = staffer == null
                ? Map.of("category", category.name())
                : Map.of("category", category.name(), "staffer", staffer.name());
        String key = staffer == null ? "gui.report.title" : "gui.report.staffer-title";
        return GuiText.single(services.messageService(), key, placeholders);
    }

    private void load(Player viewer) {
        if (staffer == null) {
            services.reportService().generalReport(viewer.getUniqueId(), category, window)
                    .thenAccept(outcome -> services.mainThreadDispatcher().runOnMainThread(() -> handleGeneral(viewer, outcome)))
                    .exceptionally(ex -> failed(viewer, ex));
        } else {
            services.reportService().stafferReport(viewer.getUniqueId(), category, window, staffer.uuid())
                    .thenAccept(outcome -> services.mainThreadDispatcher().runOnMainThread(() -> handleStaffer(viewer, outcome)))
                    .exceptionally(ex -> failed(viewer, ex));
        }
    }

    private Void failed(Player viewer, Throwable ex) {
        services.logger().log(Level.WARNING, "Report aggregation failed", ex);
        services.mainThreadDispatcher().runOnMainThread(() -> {
            CommandSupport.send(services, viewer, "general.internal-error", Map.of());
            if (!loadedOnce) {
                viewer.closeInventory();
            }
        });
        return null;
    }

    private void handleGeneral(Player viewer, ReportOutcome<GeneralReportSummary> outcome) {
        switch (outcome) {
            case ReportOutcome.Ready<GeneralReportSummary> ready -> renderGeneral(ready.summary());
            case ReportOutcome.CooldownActive<GeneralReportSummary> cooldown -> handleCooldown(viewer, cooldown.remaining());
        }
    }

    private void handleStaffer(Player viewer, ReportOutcome<StafferReportSummary> outcome) {
        switch (outcome) {
            case ReportOutcome.Ready<StafferReportSummary> ready -> renderStaffer(ready.summary());
            case ReportOutcome.CooldownActive<StafferReportSummary> cooldown -> handleCooldown(viewer, cooldown.remaining());
        }
    }

    private void handleCooldown(Player viewer, Duration remaining) {
        CommandSupport.send(services, viewer, "record.cooldown",
                Map.of("cooldown-remaining", services.messageService().formatDuration(remaining)));
        if (!loadedOnce) {
            viewer.closeInventory();
        }
    }

    private void renderWindowButtons() {
        ReportMenuConfig config = services.guiConfig().reportMenu();
        setWindowButton(config.windowDaily(), ReportWindow.DAILY, "gui.report.window-daily");
        setWindowButton(config.windowWeekly(), ReportWindow.WEEKLY, "gui.report.window-weekly");
        setWindowButton(config.windowMonthly(), ReportWindow.MONTHLY, "gui.report.window-monthly");
        setWindowButton(config.windowAllTime(), ReportWindow.ALL_TIME, "gui.report.window-all-time");
    }

    private void setWindowButton(GuiItemSlot slot, ReportWindow buttonWindow, String key) {
        String label = services.messageService().renderPlain(key, Map.of()).stream().findFirst().orElse("");
        boolean active = buttonWindow == window;
        Component name = Component.text((active ? "» " : "") + label)
                .color(active ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, active);
        setItem(slot.slot(), GuiItems.of(slot.icon(), name, List.of()));
    }

    private void renderGeneral(GeneralReportSummary summary) {
        loadedOnce = true;
        ReportMenuConfig config = services.guiConfig().reportMenu();
        clearSlots(config.rankingSlots());
        clearSlots(config.reasonSlots());
        renderSummary(summary.byState(), summary.total());
        if (summary.total() == 0) {
            return;
        }
        renderRanking(config, summary.ranking(), summary.rankingTruncated());
        renderReasons(config, summary.reasonDistribution());
    }

    private void renderStaffer(StafferReportSummary summary) {
        loadedOnce = true;
        ReportMenuConfig config = services.guiConfig().reportMenu();
        clearSlots(config.rankingSlots());
        clearSlots(config.reasonSlots());
        renderSummary(summary.byState(), summary.total());
        if (summary.total() == 0) {
            return;
        }
        renderReasons(config, summary.reasonDistribution());
        renderRecent(config, summary.recentPunishments());
    }

    private void renderSummary(StateBreakdown byState, long total) {
        Map<String, String> placeholders = Map.of(
                "total", String.valueOf(total),
                "revocation-rate", String.valueOf(Math.round(byState.revocationRatePercent())));
        Component name = GuiText.single(services.messageService(), "gui.report.total", placeholders);
        List<Component> lore = new ArrayList<>(services.messageService().render("gui.report.revocation-rate", placeholders));
        if (total == 0) {
            lore.addAll(services.messageService().render("record.no-data", Map.of()));
        }
        setItem(services.guiConfig().reportMenu().summarySlot(), GuiItems.of("BOOK", name, lore));
    }

    private void renderRanking(ReportMenuConfig config, List<StaffRanking> ranking, boolean serviceTruncated) {
        List<Integer> slots = config.rankingSlots();
        if (slots.isEmpty()) {
            return;
        }
        boolean truncated = serviceTruncated || ranking.size() > slots.size();
        int shown = truncated ? Math.min(ranking.size(), Math.max(0, slots.size() - 1)) : Math.min(ranking.size(), slots.size());
        for (int i = 0; i < slots.size(); i++) {
            if (i < shown) {
                StaffRanking entry = ranking.get(i);
                Map<String, String> placeholders = Map.of(
                        "rank", String.valueOf(i + 1),
                        "staffer", entry.staffName() != null ? entry.staffName() : "",
                        "count", String.valueOf(entry.count()));
                setItem(slots.get(i), GuiItems.of("PLAYER_HEAD",
                        GuiText.single(services.messageService(), "gui.report.ranking-entry", placeholders), List.of()));
            } else if (truncated && i == shown) {
                setItem(slots.get(i), GuiItems.of("PAPER",
                        GuiText.single(services.messageService(), "gui.report.more-entries", Map.of()), List.of()));
            }
        }
    }

    private void renderReasons(ReportMenuConfig config, List<ReasonDistributionEntry> distribution) {
        List<Integer> slots = config.reasonSlots();
        for (int i = 0; i < slots.size() && i < distribution.size(); i++) {
            ReasonDistributionEntry entry = distribution.get(i);
            Map<String, String> placeholders = Map.of("reason-display", entry.reasonDisplay(), "count", String.valueOf(entry.count()));
            setItem(slots.get(i), GuiItems.of("PAPER",
                    GuiText.single(services.messageService(), "gui.report.reason-entry", placeholders), List.of()));
        }
    }

    private void renderRecent(ReportMenuConfig config, List<Punishment> recent) {
        List<Integer> slots = config.rankingSlots();
        Instant now = services.clock().now();
        for (int i = 0; i < slots.size() && i < recent.size(); i++) {
            Punishment punishment = recent.get(i);
            Map<String, String> placeholders = PunishmentPlaceholders.build(punishment, services.messageService(), now);
            String icon = services.guiConfig().categoryMenu().items().get(punishment.category().permissionSuffix()).icon();
            setItem(slots.get(i), GuiItems.of(icon,
                    GuiText.single(services.messageService(), "gui.report.recent-entry", placeholders), List.of()));
        }
    }

    private void clearSlots(List<Integer> slots) {
        for (int slot : slots) {
            clear(slot);
        }
    }

    @Override
    void onClick(Player player, int slot, ClickType clickType) {
        ReportMenuConfig config = services.guiConfig().reportMenu();
        ReportWindow clicked = windowForSlot(config, slot);
        if (clicked != null && clicked != window) {
            window = clicked;
            renderWindowButtons();
            load(player);
        }
    }

    private static ReportWindow windowForSlot(ReportMenuConfig config, int slot) {
        if (slot == config.windowDaily().slot()) {
            return ReportWindow.DAILY;
        }
        if (slot == config.windowWeekly().slot()) {
            return ReportWindow.WEEKLY;
        }
        if (slot == config.windowMonthly().slot()) {
            return ReportWindow.MONTHLY;
        }
        if (slot == config.windowAllTime().slot()) {
            return ReportWindow.ALL_TIME;
        }
        return null;
    }
}
