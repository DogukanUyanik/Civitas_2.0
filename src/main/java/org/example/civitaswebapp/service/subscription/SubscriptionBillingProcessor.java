package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bills a single due member inside its OWN transaction. Living in a separate bean from the
 * orchestrator loop is deliberate: it guarantees the {@code @Transactional} boundary is honoured
 * per member (Spring's proxy is bypassed on self-invocation), so one member's rollback never
 * affects another's. If any step throws, the whole member's work rolls back atomically — no
 * orphaned transaction and no advanced billing date — and the member is naturally retried on the
 * next run.
 */
@Component
public class SubscriptionBillingProcessor {

    private final MemberRepository memberRepository;
    private final TransactionService transactionService;
    private final WhatsAppService whatsAppService;

    public SubscriptionBillingProcessor(MemberRepository memberRepository,
                                        TransactionService transactionService,
                                        WhatsAppService whatsAppService) {
        this.memberRepository = memberRepository;
        this.transactionService = transactionService;
        this.whatsAppService = whatsAppService;
    }

    @Transactional
    public void processDueMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Member not found: " + memberId));

        BigDecimal amount = member.getSubscriptionAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException(
                    "Member " + memberId + " has no valid subscription amount configured.");
        }

        // 1. Persist the PENDING charge first — the Stripe link references the transaction id.
        Transaction transaction = transactionService.createSubscriptionTransaction(member, amount.doubleValue());

        // 2. Generate the Stripe checkout link for this charge.
        String paymentLink = transactionService.generateStripePaymentLink(transaction);

        // 3. Deliver the link over WhatsApp. An invalid/unroutable number throws here, which rolls
        //    back this member's transaction so nothing half-finished is left behind.
        whatsAppService.sendPaymentLink(member.getPhoneNumber(), paymentLink);

        // 4. Advance the billing clock from the CURRENT nextBillingDate (not "today"), so cycles
        //    stay anchored to the original schedule even if a run is late.
        LocalDate current = member.getNextBillingDate();
        member.setNextBillingDate(member.getSubscriptionFrequency().nextBillingDate(current));
        memberRepository.save(member);
    }
}
