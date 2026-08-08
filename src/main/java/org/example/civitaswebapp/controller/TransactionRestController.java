package org.example.civitaswebapp.controller;

import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/transactions")
public class TransactionRestController {

    private static final Logger log = LoggerFactory.getLogger(TransactionRestController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private MessageSource messageSource;


    @PostMapping("/send-payment")
    public ResponseEntity<Map<String, Object>> sendPayment(
            @RequestParam Long memberId,
            @RequestParam double amount,
            @RequestParam String currency,
            @RequestParam String paymentType,
            @RequestParam(required = false) String note
    ) {
        Map<String, Object> response = new HashMap<>();

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            response.put("success", false);
            response.put("message", "Member not found in Database");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            TransactionType transactionType = TransactionType.valueOf(paymentType);
            MyUser createdByUser = myUserService.getLoggedInUser();
            Transaction transaction = transactionService.createTransaction(member, amount, transactionType, createdByUser);
            transaction.setCurrency(currency);

            if (note != null && !note.isEmpty()) {
                transaction.setNote(note);
            }


            String paymentLink = transactionService.generateStripePaymentLink(transaction);

            // The transaction itself is created regardless of WhatsApp delivery. Track delivery
            // separately so the UI can avoid showing a false "success" toast when Twilio fails,
            // and classify the failure so the message names the actual cause instead of always
            // blaming the phone number. See WhatsAppService for the exception contract.
            boolean whatsappSuccess = false;
            if (member.getPhoneNumber() == null || member.getPhoneNumber().isBlank()) {
                log.warn("No WhatsApp sent for member {}: no phone number on file", member.getId());
                reportWhatsappError(response, "PHONE_MISSING", "payment.whatsapp.error.phoneMissing", null);
            } else {
                try {
                    whatsAppService.sendPaymentLink(member, paymentLink);
                    whatsappSuccess = true;
                } catch (IllegalArgumentException wa) {
                    log.warn("No WhatsApp sent for member {}: {}", member.getId(), wa.getMessage());
                    reportWhatsappError(response, "PHONE_INVALID", "payment.whatsapp.error.phoneInvalid", null);
                } catch (ApiException wa) {
                    Object code = wa.getCode() != null ? wa.getCode() : wa.getStatusCode();
                    log.warn("Twilio rejected WhatsApp for member {}: code={} status={} moreInfo={}",
                            member.getId(), wa.getCode(), wa.getStatusCode(), wa.getMoreInfo(), wa);
                    reportWhatsappError(response, "TWILIO_REJECTED", "payment.whatsapp.error.provider",
                            new Object[]{code});
                } catch (TwilioException wa) {
                    // ApiConnectionException and friends: Twilio was unreachable. Still an external
                    // failure, not a config problem — must not be reported as one.
                    log.warn("Could not reach Twilio for member {}: {}", member.getId(), wa.getMessage(), wa);
                    reportWhatsappError(response, "TWILIO_REJECTED", "payment.whatsapp.error.provider",
                            new Object[]{"-"});
                } catch (IllegalStateException wa) {
                    log.error("WhatsApp misconfigured for member {}: {}", member.getId(), wa.getMessage(), wa);
                    reportWhatsappError(response, "SYSTEM_ERROR", "payment.whatsapp.error.system", null);
                } catch (Exception wa) {
                    log.error("Unexpected WhatsApp failure for member {}", member.getId(), wa);
                    reportWhatsappError(response, "SYSTEM_ERROR", "payment.whatsapp.error.system", null);
                }
            }

            response.put("success", true);
            response.put("whatsappSuccess", whatsappSuccess);
            response.put("paymentLink", paymentLink);
            response.put("transactionId", transaction.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create payment for member {}", memberId, e);
            response.put("success", false);
            response.put("message", "Critical Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Records a WhatsApp delivery failure on the response.
     *
     * <p>Two fields on purpose: {@code whatsapp_error} is the localized text the browser shows, and
     * {@code whatsapp_error_code} is a stable token so tests and any future UI branching assert on
     * something that does not move when a translation is reworded.
     */
    private void reportWhatsappError(Map<String, Object> response, String code,
                                     String messageKey, Object[] args) {
        response.put("whatsapp_error_code", code);
        response.put("whatsapp_error",
                messageSource.getMessage(messageKey, args, LocaleContextHolder.getLocale()));
    }

    /**
     * Settles a PENDING transaction as a manual/cash payment, bypassing Stripe. Union scoping and
     * the pending-state guard live in the service layer.
     */
    @PostMapping("/{id}/mark-cash")
    public ResponseEntity<Map<String, Object>> markAsCash(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            transactionService.markTransactionAsCash(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

