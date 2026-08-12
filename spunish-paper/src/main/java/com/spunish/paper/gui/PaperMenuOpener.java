package com.spunish.paper.gui;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.paper.SPunishServices;
import com.spunish.paper.command.MenuOpener;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class PaperMenuOpener implements MenuOpener {

    private final SPunishServices services;

    public PaperMenuOpener(SPunishServices services) {
        this.services = services;
    }

    @Override
    public void openCategoryMenu(Player player, PunishmentTarget target) {
        new CategoryMenu(services, player, target).open(player);
    }

    @Override
    public void openReasonMenu(Player player, PunishmentTarget target, PunishmentCategory category) {
        new ReasonMenu(services, player, target, category, false, 0).open(player);
    }

    @Override
    public void openHistoryMenu(Player player, PunishmentTarget target) {
        new HistoryMenu(services, player, target, Optional.empty(), 0).open(player);
    }

    @Override
    public void openGeneralReportMenu(Player player, PunishmentCategory category) {
        new ReportMenu(services, player, category, null).open(player);
    }

    @Override
    public void openStafferReportMenu(Player player, PunishmentCategory category, PunishmentTarget staffer) {
        new ReportMenu(services, player, category, staffer).open(player);
    }
}
