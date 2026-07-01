package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.example.civitaswebapp.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
    private final SubscriptionBillingProcessor processor;

    public SubscriptionBillingServiceImpl(MemberRepository memberRepository,
                                          SubscriptionBillingProcessor processor) {
        this.memberRepository = memberRepository;
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
}
