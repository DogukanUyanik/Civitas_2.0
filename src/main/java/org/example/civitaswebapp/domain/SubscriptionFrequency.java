package org.example.civitaswebapp.domain;

import java.time.LocalDate;

/**
 * Recurring-billing cadence for a member's membership fee. The scheduler (Phase 2) advances
 * {@code Member.nextBillingDate} by this cadence after each charge.
 */
public enum SubscriptionFrequency {
    NONE,
    MONTHLY,
    YEARLY;

    /**
     * The next billing date relative to {@code from} for this cadence, or {@code null} for
     * {@link #NONE} (no recurring billing). Keeping the date arithmetic on the enum makes it the
     * single source of truth for both initial scheduling (Phase 1) and the scheduler (Phase 2).
     */
    public LocalDate nextBillingDate(LocalDate from) {
        if (from == null) {
            return null;
        }
        return switch (this) {
            case MONTHLY -> from.plusMonths(1);
            case YEARLY -> from.plusYears(1);
            case NONE -> null;
        };
    }
}
