package com.spunish.common.storage;

public record StateBreakdown(long active, long expired, long revoked) {

    public long total() {
        return active + expired + revoked;
    }

    /**
     * Revoked punishments still count toward {@link #total()} — this is
     * revoked-over-total, not revoked-over-(everything-else).
     */
    public double revocationRatePercent() {
        long total = total();
        return total == 0 ? 0.0 : (revoked * 100.0) / total;
    }
}
