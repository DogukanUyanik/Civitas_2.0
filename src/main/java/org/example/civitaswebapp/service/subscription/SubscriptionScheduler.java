package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the automated subscription engine. Scheduling is enabled globally via
 * {@code @EnableScheduling} on the application class. The actual billing logic lives in
 * {@link SubscriptionBillingService} so it stays unit-testable and reusable by the admin trigger
 * endpoint.
 */
@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionBillingService billingService;

    public SubscriptionScheduler(SubscriptionBillingService billingService) {
        this.billingService = billingService;
    }

    /** Runs every day at 02:00 (server time), a quiet background hour. */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySubscriptionBilling() {
        log.info("Scheduled subscription billing run starting...");
        SubscriptionBillingResult result = billingService.runDueSubscriptions();
        log.info("Scheduled subscription billing run finished: {}", result);
    }
}
