package org.example.civitaswebapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Unauthenticated landing pages a member's phone browser lands on after Stripe Checkout.
 * Deliberately does NOT query the database or render transaction details (amount/status) —
 * these routes are public and transactionId is a guessable sequential id. Real status updates
 * happen server-side via {@link StripeWebhookController}; these pages are UI confirmation only.
 */
@Controller
public class PaymentPageController {

    private static final Logger log = LoggerFactory.getLogger(PaymentPageController.class);

    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam(required = false) Long transactionId) {
        log.info("Payment success page viewed for transactionId={}", transactionId);
        return "payment-success";
    }

    @GetMapping("/payment-cancel")
    public String paymentCancel() {
        return "payment-cancel";
    }
}
