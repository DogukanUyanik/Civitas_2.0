package org.example.civitaswebapp.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the billing-cadence arithmetic that drives both initial scheduling (Phase 1) and the
 * scheduler (Phase 2).
 */
class SubscriptionFrequencyTest {

    private final LocalDate start = LocalDate.of(2026, 6, 30);

    @Test
    void monthlyAddsOneMonth() {
        assertThat(SubscriptionFrequency.MONTHLY.nextBillingDate(start))
                .isEqualTo(LocalDate.of(2026, 7, 30));
    }

    @Test
    void yearlyAddsOneYear() {
        assertThat(SubscriptionFrequency.YEARLY.nextBillingDate(start))
                .isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void noneHasNoNextBillingDate() {
        assertThat(SubscriptionFrequency.NONE.nextBillingDate(start)).isNull();
    }

    @Test
    void nullStartYieldsNull() {
        assertThat(SubscriptionFrequency.MONTHLY.nextBillingDate(null)).isNull();
    }

    @Test
    void monthlyHandlesEndOfMonthRollover() {
        // 31 Jan + 1 month -> 28 Feb (java.time clamps to the last valid day).
        assertThat(SubscriptionFrequency.MONTHLY.nextBillingDate(LocalDate.of(2026, 1, 31)))
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
