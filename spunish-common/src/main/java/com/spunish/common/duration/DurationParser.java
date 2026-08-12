package com.spunish.common.duration;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the {@code <time>} argument shared by reason durations and the
 * {@code /punish} command: the literal {@code n} for permanent, or one or
 * more {@code quantity+unit} pairs with no separators (for example
 * {@code 30d} or {@code 1y6mo}). {@code mo} is checked before the
 * single-letter units so {@code mo} is never split into {@code m} + a
 * dangling {@code o}.
 */
public final class DurationParser {

    private static final String PERMANENT_LITERAL = "n";
    private static final Pattern TOKEN = Pattern.compile("(\\d+)(mo|s|m|h|d|w|y)");

    public Optional<PunishmentDuration> parse(String input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        if (input.equals(PERMANENT_LITERAL)) {
            return Optional.of(PunishmentDuration.Permanent.INSTANCE);
        }

        Matcher matcher = TOKEN.matcher(input);
        long totalSeconds = 0;
        int position = 0;
        try {
            while (position < input.length()) {
                matcher.region(position, input.length());
                if (!matcher.lookingAt()) {
                    return Optional.empty();
                }
                long quantity = Long.parseLong(matcher.group(1));
                DurationUnit unit = DurationUnit.fromCode(matcher.group(2)).orElseThrow();
                totalSeconds = Math.addExact(totalSeconds, Math.multiplyExact(quantity, unit.seconds()));
                position = matcher.end();
            }
        } catch (ArithmeticException | NumberFormatException overflow) {
            return Optional.empty();
        }

        if (totalSeconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PunishmentDuration.Temporary(Duration.ofSeconds(totalSeconds)));
    }
}
