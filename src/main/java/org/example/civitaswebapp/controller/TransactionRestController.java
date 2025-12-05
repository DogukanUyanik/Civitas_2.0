package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/transactions")
public class TransactionRestController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private WhatsAppService whatsAppService;

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
            response.put("message", "Member not found in Database"); // Updated for clarity
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Create transaction
            TransactionType transactionType = TransactionType.valueOf(paymentType);
            Transaction transaction = transactionService.createTransaction(member, amount, transactionType);
            transaction.setCurrency(currency);

            if (note != null && !note.isEmpty()) {
                transaction.setNote(note);
            }

            // 2. Generate Stripe payment link
            String paymentLink = transactionService.generateStripePaymentLink(transaction);

            // 3. Send WhatsApp message (ISOLATED)
            try {
                if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                    whatsAppService.sendPaymentLink(member.getPhoneNumber(), paymentLink);
                }
            } catch (Exception wa) {
                // Log the error but DO NOT stop the response.
                System.err.println("WhatsApp failed: " + wa.getMessage());
                response.put("whatsapp_error", "Message could not be sent: " + wa.getMessage());
            }

            // 4. Return success (Payment link was generated successfully)
            response.put("success", true);
            response.put("paymentLink", paymentLink);
            response.put("transactionId", transaction.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // This catch now only handles Stripe or Database failures
            e.printStackTrace(); // PRINT THE STACK TRACE TO CONSOLE
            response.put("success", false);
            response.put("message", "Critical Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

