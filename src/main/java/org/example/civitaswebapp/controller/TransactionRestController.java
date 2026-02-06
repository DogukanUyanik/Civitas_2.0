package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
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

    @Autowired
    private MyUserService myUserService;


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

            try {
                if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                    whatsAppService.sendPaymentLink(member.getPhoneNumber(), paymentLink);
                }
            } catch (Exception wa) {

                System.err.println("WhatsApp failed: " + wa.getMessage());
                response.put("whatsapp_error", "Message could not be sent: " + wa.getMessage());
            }


            response.put("success", true);
            response.put("paymentLink", paymentLink);
            response.put("transactionId", transaction.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace(); // PRINT THE STACK TRACE TO CONSOLE
            response.put("success", false);
            response.put("message", "Critical Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

