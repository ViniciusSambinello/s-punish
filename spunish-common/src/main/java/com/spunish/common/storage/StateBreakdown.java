package com.spunish.common.storage;

public record StateBreakdown(long active, long expired, long revoked) {

    public long total() {
        return active + expired + revoked;
    }
}
