package com.spunish.common.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StateBreakdownTest {

    @Test
    void revokedPunishmentsStillCountTowardTheTotal() {
        StateBreakdown breakdown = new StateBreakdown(7, 0, 3);

        assertThat(breakdown.total()).isEqualTo(10);
        assertThat(breakdown.revocationRatePercent()).isCloseTo(30.0, within(0.001));
    }

    @Test
    void emptyBreakdownHasZeroRate() {
        StateBreakdown breakdown = new StateBreakdown(0, 0, 0);

        assertThat(breakdown.revocationRatePercent()).isZero();
    }
}
