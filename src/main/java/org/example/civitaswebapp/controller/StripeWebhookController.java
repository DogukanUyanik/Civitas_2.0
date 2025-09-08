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
                    logger.info("Handling checkout.session.completed event");
                    handleCheckoutSessionCompleted(event);
                    break;

                case "payment_intent.payment_failed":
                    logger.info("Handling payment_intent.payment_failed event");
                    handlePaymentIntentFailed(event);
                    break;

                case "payment_intent.requires_action":
                    logger.info("Received payment_intent.requires_action event - no action needed");
                    break;

                case "payment_intent.created":
                    logger.info("Received payment_intent.created event - no action needed");
                    break;

                default:
                    logger.debug("Unhandled event type: {}", event.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }

        logger.info("Successfully processed webhook event: {}", event.getType());
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
            logger.info("Processing payment_intent.payment_failed event: {}", event.getId());

            com.stripe.model.PaymentIntent failedIntent = null;

            // Try DataObjectDeserializer first
            var optionalObject = event.getDataObjectDeserializer().getObject();
            if (optionalObject.isPresent() && optionalObject.get() instanceof com.stripe.model.PaymentIntent) {
                failedIntent = (com.stripe.model.PaymentIntent) optionalObject.get();
                logger.debug("Successfully deserialized PaymentIntent using DataObjectDeserializer");
            } else {
                // Fallback to direct access if deserializer returns empty
                Object dataObject = event.getData().getObject();
                if (dataObject instanceof com.stripe.model.PaymentIntent) {
                    failedIntent = (com.stripe.model.PaymentIntent) dataObject;
                    logger.debug("Successfully deserialized PaymentIntent using direct access");
                } else {
                    logger.error("Failed to deserialize PaymentIntent from event data. Object type: {}",
                            dataObject != null ? dataObject.getClass().getName() : "null");
                    return;
                }
            }

            logger.info("PaymentIntent ID: {}", failedIntent.getId());
            logger.info("PaymentIntent metadata: {}", failedIntent.getMetadata());

            String transactionIdStr = failedIntent.getMetadata().get("transactionId");
            if (transactionIdStr != null && !transactionIdStr.isEmpty()) {
                try {
                    Long transactionId = Long.parseLong(transactionIdStr);
                    logger.info("Found transactionId {} in PaymentIntent metadata", transactionId);

                    transactionService.updateTransactionStatus(transactionId, TransactionStatus.FAILED);
                    logger.info("Transaction {} marked as FAILED for payment intent {}", transactionId, failedIntent.getId());
                } catch (NumberFormatException e) {
                    logger.error("Invalid transactionId format in metadata: {}", transactionIdStr, e);
                }
            } else {
                logger.warn("PaymentIntent failed but no transactionId found in metadata for payment intent {}", failedIntent.getId());
                logger.warn("Available metadata keys: {}", failedIntent.getMetadata().keySet());
            }

        } catch (Exception e) {
            logger.error("Error processing payment_intent.payment_failed event: {}", e.getMessage(), e);
            throw e;
        }
    }
}