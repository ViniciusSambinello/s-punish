package com.spunish.paper.command;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import org.bukkit.entity.Player;

/**
 * Opens every GUI a command can lead to. Implemented for real in section 9;
 * {@link StubMenuOpener} stands in until then.
 */
public interface MenuOpener {

    static MenuOpener stub() {
        return new StubMenuOpener();
    }

    void openCategoryMenu(Player player, PunishmentTarget target);

    void openReasonMenu(Player player, PunishmentTarget target, PunishmentCategory category);

    void openHistoryMenu(Player player, PunishmentTarget target);

    void openGeneralReportMenu(Player player, PunishmentCategory category);

    void openStafferReportMenu(Player player, PunishmentCategory category, PunishmentTarget staffer);
}
