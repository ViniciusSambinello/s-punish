package com.spunish.common.service;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentState;

public record HistoryEntry(Punishment punishment, PunishmentState state) {
}
