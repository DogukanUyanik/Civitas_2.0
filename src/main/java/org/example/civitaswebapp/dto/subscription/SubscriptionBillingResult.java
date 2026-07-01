package org.example.civitaswebapp.dto.subscription;

/**
 * Summary of a single subscription-billing run: how many members were due, how many were billed
 * successfully, and how many failed (and were skipped without aborting the run).
 */
public record SubscriptionBillingResult(int due, int succeeded, int failed) {
}
