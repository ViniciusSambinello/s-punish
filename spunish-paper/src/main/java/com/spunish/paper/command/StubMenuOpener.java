package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Placeholder until section 9 lands the real GUIs — the command surface
 * (arity dispatch, permissions, tab complete) is fully usable via the
 * complete/direct form in the meantime.
 */
final class StubMenuOpener implements MenuOpener {

    @Override
    public void openCategoryMenu(Player player, PunishmentTarget target) {
        notYetAvailable(player);
    }

    @Override
    public void openReasonMenu(Player player, PunishmentTarget target, PunishmentCategory category) {
        notYetAvailable(player);
    }

    @Override
    public void openHistoryMenu(Player player, PunishmentTarget target) {
        notYetAvailable(player);
    }

    @Override
    public void openGeneralReportMenu(Player player, PunishmentCategory category) {
        notYetAvailable(player);
    }

    @Override
    public void openStafferReportMenu(Player player, PunishmentCategory category, PunishmentTarget staffer) {
        notYetAvailable(player);
    }

    private void notYetAvailable(Player player) {
        player.sendMessage(Component.text(
                "GUI not available yet in this build — use /punish <player> <category> <reason> <time>."));
    }
}
