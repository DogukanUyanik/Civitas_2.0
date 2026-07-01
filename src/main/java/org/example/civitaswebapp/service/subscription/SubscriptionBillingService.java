package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;

public interface SubscriptionBillingService {

    /**
     * Bills every member whose active subscription is due today (across all unions). Each member is
     * processed independently: a failure for one member is logged and skipped, never aborting the
     * run. Returns a summary of the run.
     */
    SubscriptionBillingResult runDueSubscriptions();
}
