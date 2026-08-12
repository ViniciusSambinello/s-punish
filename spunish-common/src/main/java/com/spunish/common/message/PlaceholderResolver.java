package com.spunish.common.message;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {token}} placeholders. An unknown token is left literal
 * in the output — the message still gets sent — and reported once via the
 * supplied callback rather than raising.
 */
public final class PlaceholderResolver {

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");

    public String resolve(String raw, Map<String, String> values, Consumer<String> onUnknownPlaceholder) {
        Matcher matcher = TOKEN.matcher(raw);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(raw, lastEnd, matcher.start());
            String token = matcher.group(1);
            String value = values.get(token);
            if (value != null) {
                result.append(value);
            } else {
                onUnknownPlaceholder.accept(token);
                result.append(matcher.group());
            }
            lastEnd = matcher.end();
        }
        result.append(raw, lastEnd, raw.length());
        return result.toString();
    }
}
