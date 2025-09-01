package org.example.civitaswebapp.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    private final TransactionService transactionService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public StripeWebhookController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
                                                    @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook signature verification failed");
        }

        logger.info("Processing Stripe event: {} ({})", event.getType(), event.getId());

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;

                default:
                    logger.debug("Unhandled event type: {}", event.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }

        return ResponseEntity.ok("Received");
    }

    private void handleCheckoutSessionCompleted(Event event) {
        try {
            Session session;

            // Try DataObjectDeserializer first
            var optionalObject = event.getDataObjectDeserializer().getObject();
            if (optionalObject.isPresent() && optionalObject.get() instanceof Session) {
                session = (Session) optionalObject.get();
            } else {
                // Fallback to direct access if deserializer returns empty
                session = (Session) event.getData().getObject();
            }

            String transactionIdStr = session.getMetadata().get("transactionId");
            if (transactionIdStr != null && !transactionIdStr.isEmpty()) {
                Long transactionId = Long.parseLong(transactionIdStr);
                transactionService.updateTransactionStatus(transactionId, TransactionStatus.SUCCEEDED);
                logger.info("Transaction {} marked as SUCCEEDED for session {}", transactionId, session.getId());
            } else {
                logger.warn("No transactionId found in session metadata for session {}", session.getId());
            }

        } catch (Exception e) {
            logger.error("Error processing checkout.session.completed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        try {
            var optionalObject = event.getDataObjectDeserializer().getObject();
            if (optionalObject.isPresent() && optionalObject.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent failedIntent = (com.stripe.model.PaymentIntent) optionalObject.get();

                String transactionIdStr = failedIntent.getMetadata().get("transactionId");
                if (transactionIdStr != null && !transactionIdStr.isEmpty()) {
                    Long transactionId = Long.parseLong(transactionIdStr);
                    transactionService.updateTransactionStatus(transactionId, TransactionStatus.FAILED);
                    logger.info("Transaction {} marked as FAILED for payment intent {}", transactionId, failedIntent.getId());
                } else {
                    logger.warn("PaymentIntent failed but no transactionId found in metadata for payment intent {}", failedIntent.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing payment_intent.payment_failed event: {}", e.getMessage(), e);
            throw e;
        }
    }
}