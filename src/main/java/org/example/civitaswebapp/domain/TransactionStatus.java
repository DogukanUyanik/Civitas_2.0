package org.example.civitaswebapp.domain;

import java.util.List;

public enum TransactionStatus {
    PENDING,
    SUCCEEDED,
    /** Settled manually in cash by an admin/treasurer — counts as revenue, but kept distinct from
     *  Stripe online payments ({@link #SUCCEEDED}) for accounting/KPI reporting. */
    PAID_MANUALLY,
    FAILED,
    EXPIRED;

    /**
     * Statuses that represent money actually received — both Stripe online ({@link #SUCCEEDED}) and
     * manual cash ({@link #PAID_MANUALLY}). Revenue aggregations must use this set so cash in the
     * register is included in totals while remaining individually distinguishable.
     */
    public static List<TransactionStatus> revenueStatuses() {
        return List.of(SUCCEEDED, PAID_MANUALLY);
    }
}
