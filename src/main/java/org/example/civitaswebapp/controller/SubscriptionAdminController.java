package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.example.civitaswebapp.service.subscription.SubscriptionBillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only on-demand trigger for the subscription billing job, so the engine can be verified
 * without waiting for the 02:00 cron. Authorization (ROLE_ADMIN) is enforced in SecurityConfig for
 * {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/subscriptions")
public class SubscriptionAdminController {

    private final SubscriptionBillingService billingService;

    public SubscriptionAdminController(SubscriptionBillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/trigger-job")
    public ResponseEntity<SubscriptionBillingResult> triggerBillingJob() {
        return ResponseEntity.ok(billingService.runDueSubscriptions());
    }
}
