package org.example.civitaswebapp.domain;

/**
 * Lifecycle state of a member's recurring subscription. Distinct from {@link SubscriptionStatus},
 * which models the tenant union's own SaaS plan. {@link #PAUSED} lets an admin temporarily stop
 * automated billing without discarding the subscription configuration.
 */
public enum MemberSubscriptionStatus {
    ACTIVE,
    PAUSED
}
