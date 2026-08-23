package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bills a single due member inside its OWN transaction. Living in a separate bean from the
 * orchestrator loop is deliberate: it guarantees the {@code @Transactional} boundary is honoured
 * per member (Spring's proxy is bypassed on self-invocation), so one member's rollback never
 * affects another's. If any step throws, the whole member's work rolls back atomically — no
 * orphaned transaction and no advanced billing date — and the member is naturally retried on the
 * next run.
 *
 * <p>Guards against re-billing a member who already has an unresolved PENDING charge: instead of
 * unconditionally creating a new transaction/Stripe link/WhatsApp message every run (the root
 * cause of a production incident where a member was messaged nightly for 10+ consecutive days),
 * an existing PENDING charge is reused — reminded on a cooldown while inside its grace period, and
 * only superseded by a fresh charge once it expires.
 */
@Component
public class SubscriptionBillingProcessor {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final WhatsAppService whatsAppService;

    @Value("${subscription.billing.pending-grace-period-days}")
    private int pendingGracePeriodDays;

    @Value("${subscription.billing.reminder-cooldown-days}")
    private int reminderCooldownDays;

    public SubscriptionBillingProcessor(MemberRepository memberRepository,
                                        TransactionRepository transactionRepository,
                                        TransactionService transactionService,
                                        WhatsAppService whatsAppService) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
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

        Optional<Transaction> existingPending = transactionRepository
                .findFirstByMemberAndTypeAndStatusOrderByCreatedAtDesc(
                        member, TransactionType.MEMBERSHIP_FEE, TransactionStatus.PENDING);

        if (existingPending.isPresent() && !handleExistingPending(member, existingPending.get())) {
            // Still within its grace period: either a reminder was (re)sent or the cooldown is
            // still active. Either way, no new charge yet and the billing clock does not move.
            return;
        }

        // 1. Persist the PENDING charge first — the Stripe link references the transaction id.
        Transaction transaction = transactionService.createSubscriptionTransaction(member, amount.doubleValue());

        // 2. Generate the Stripe checkout link for this charge.
        String paymentLink = transactionService.generateStripePaymentLink(transaction);

        // 3. Deliver the link over WhatsApp. An invalid/unroutable number throws here, which rolls
        //    back this member's transaction so nothing half-finished is left behind.
        whatsAppService.sendPaymentLink(member, paymentLink);

        // 4. Advance the billing clock, fast-forwarding past any fully-elapsed cycles so a member
        //    who fell behind (stale data, downtime, or a just-expired unpaid cycle) is billed once
        //    for the current cycle rather than once per missed cycle.
        member.setNextBillingDate(nextFutureBillingDate(member));
        memberRepository.save(member);
    }

    /**
     * Handles an already-outstanding PENDING charge. Returns {@code true} if it was expired and
     * the caller should proceed to bill a fresh cycle, or {@code false} if the existing charge is
     * still within its grace period (a reminder may have been sent, subject to the cooldown).
     */
    private boolean handleExistingPending(Member member, Transaction pending) {
        LocalDateTime now = LocalDateTime.now();
        long ageDays = Duration.between(pending.getCreatedAt(), now).toDays();

        if (ageDays < pendingGracePeriodDays) {
            LocalDateTime lastContact = pending.getLastReminderSentAt() != null
                    ? pending.getLastReminderSentAt() : pending.getCreatedAt();
            long daysSinceLastContact = Duration.between(lastContact, now).toDays();

            if (daysSinceLastContact >= reminderCooldownDays) {
                // Stripe Checkout Sessions expire (~24h by default), so a resend needs a fresh
                // session — but it stays tied to the SAME transaction, not a new one.
                String paymentLink = transactionService.generateStripePaymentLink(pending);
                whatsAppService.sendPaymentLink(member, paymentLink);
                pending.setLastReminderSentAt(now);
                transactionRepository.save(pending);
            }
            return false;
        }

        // Past the grace period: give up on collecting this specific cycle so the member can be
        // billed for the current cycle instead of being stuck behind one old unpaid invoice.
        pending.setStatus(TransactionStatus.EXPIRED);
        pending.setUpdatedAt(now);
        transactionRepository.save(pending);
        return true;
    }

    private static LocalDate nextFutureBillingDate(Member member) {
        LocalDate today = LocalDate.now();
        LocalDate next = member.getSubscriptionFrequency().nextBillingDate(member.getNextBillingDate());
        while (next != null && !next.isAfter(today)) {
            next = member.getSubscriptionFrequency().nextBillingDate(next);
        }
        return next;
    }
}
