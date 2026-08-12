package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import org.bukkit.entity.Player;

/**
 * Opens every GUI a command can lead to.
 */
public interface MenuOpener {

    void openCategoryMenu(Player player, PunishmentTarget target);

    void openReasonMenu(Player player, PunishmentTarget target, PunishmentCategory category);

    void openHistoryMenu(Player player, PunishmentTarget target);

    void openGeneralReportMenu(Player player, PunishmentCategory category);

    void openStafferReportMenu(Player player, PunishmentCategory category, PunishmentTarget staffer);
}
