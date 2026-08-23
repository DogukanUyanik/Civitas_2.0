package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates a subscription-billing run. Intentionally NOT transactional: it reads the due-member
 * list, then delegates each member to {@link SubscriptionBillingProcessor} (a separate bean) so
 * every member runs in its own transaction. The per-member try/catch is the resilience guard — one
 * member's failure (bad phone, Stripe error, missing amount) is logged and skipped so the rest
 * still get billed.
 */
@Service
public class SubscriptionBillingServiceImpl implements SubscriptionBillingService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionBillingServiceImpl.class);

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionBillingProcessor processor;

    public SubscriptionBillingServiceImpl(MemberRepository memberRepository,
                                          TransactionRepository transactionRepository,
                                          SubscriptionBillingProcessor processor) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
        this.processor = processor;
    }

    @Override
    public SubscriptionBillingResult runDueSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Member> dueMembers = memberRepository.findDueSubscriptions(
                MemberSubscriptionStatus.ACTIVE, SubscriptionFrequency.NONE, today);

        log.info("Subscription billing: {} member(s) due on or before {}", dueMembers.size(), today);

        int succeeded = 0;
        int failed = 0;
        for (Member member : dueMembers) {
            try {
                processor.processDueMember(member.getId());
                succeeded++;
            } catch (Exception e) {
                // Resilience: log and continue so a single bad member never aborts the batch.
                failed++;
                log.error("Subscription billing failed for member id={} ({}): {}",
                        member.getId(), member.getName(), e.getMessage(), e);
            }
        }

        log.info("Subscription billing complete: {} succeeded, {} failed of {} due",
                succeeded, failed, dueMembers.size());
        return new SubscriptionBillingResult(dueMembers.size(), succeeded, failed);
    }

    @Override
    @Transactional
    public int cleanupDuplicatePendingSubscriptionTransactions() {
        List<Transaction> pending = transactionRepository.findAllByTypeAndStatusOrderByMemberIdAscCreatedAtDesc(
                TransactionType.MEMBERSHIP_FEE, TransactionStatus.PENDING);

        Map<Long, List<Transaction>> byMember = pending.stream()
                .collect(Collectors.groupingBy(t -> t.getMember().getId(), LinkedHashMap::new, Collectors.toList()));

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;
        int affectedMembers = 0;
        for (List<Transaction> memberTransactions : byMember.values()) {
            if (memberTransactions.size() <= 1) {
                continue;
            }
            affectedMembers++;
            // Query orders each member's group by createdAt desc, so index 0 is the most recent —
            // keep it, expire the rest.
            for (Transaction stale : memberTransactions.subList(1, memberTransactions.size())) {
                stale.setStatus(TransactionStatus.EXPIRED);
                stale.setUpdatedAt(now);
                transactionRepository.save(stale);
                expiredCount++;
            }
        }

        log.info("Duplicate pending subscription cleanup: expired {} stale transaction(s) across {} member(s)",
                expiredCount, affectedMembers);
        return expiredCount;
    }
}
