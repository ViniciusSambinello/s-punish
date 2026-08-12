package com.spunish.common.message;

import com.spunish.common.duration.DurationFormatSpec;
import com.spunish.common.duration.DurationUnit;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

record MessagesSnapshot(
        ConfigurationNode root,
        String prefix,
        DateTimeFormatterService dateTimeFormatter,
        DurationFormatSpec durationFormat) {

    static MessagesSnapshot from(ConfigurationNode root) {
        String prefix = root.node("prefix").getString("");

        String pattern = root.node("date-time", "pattern").getString("dd/MM/yyyy HH:mm");
        String zoneId = root.node("date-time", "zone").getString("UTC");
        DateTimeFormatterService dateTimeFormatter = new DateTimeFormatterService(pattern, ZoneId.of(zoneId));

        int maxUnits = root.node("duration", "max-units").getInt(2);
        String permanentText = root.node("duration", "permanent-text").getString("permanent");
        Map<DurationUnit, String> labels = new EnumMap<>(DurationUnit.class);
        for (DurationUnit unit : DurationUnit.values()) {
            labels.put(unit, root.node("duration", "units", unit.code()).getString(unit.code()));
        }
        DurationFormatSpec durationFormat = new DurationFormatSpec(labels, maxUnits, permanentText);

        return new MessagesSnapshot(root, prefix, dateTimeFormatter, durationFormat);
    }
}
