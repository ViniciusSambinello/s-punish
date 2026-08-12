package com.spunish.common.storage;

import java.util.regex.Pattern;

public final class TableNames {

    private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Za-z0-9_]*");

    private final String prefix;

    public TableNames(String prefix) {
        if (!SAFE_PREFIX.matcher(prefix).matches()) {
            throw new IllegalArgumentException(
                    "database.table-prefix must contain only letters, digits and underscores: '" + prefix + "'");
        }
        this.prefix = prefix;
    }

    public String profiles() {
        return prefix + "profiles";
    }

    public String punishments() {
        return prefix + "punishments";
    }

    public String syncEvents() {
        return prefix + "sync_events";
    }

    public String schemaVersion() {
        return prefix + "schema_version";
    }
}
