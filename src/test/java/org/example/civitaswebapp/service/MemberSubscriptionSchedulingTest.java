package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Phase-1 subscription scheduling contract in {@code MemberServiceImpl.saveMember}:
 * enabling a subscription derives the first {@code nextBillingDate} from the start date + cadence,
 * NONE clears the schedule, and an already-scheduled member is never silently re-scheduled.
 */
@ExtendWith(MockitoExtension.class)
class MemberSubscriptionSchedulingTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MyUserService myUserService;

    @InjectMocks
    private MemberServiceImpl memberService;

    private MyUser user() {
        Union union = Union.builder().id(UUID.randomUUID()).name("Demo").build();
        return MyUser.builder().id(1L).union(union).build();
    }

    private Member.MemberBuilder baseMember() {
        return Member.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .phoneNumber("+32470000000")
                .address("1 Main St")
                .memberStatus(MemberStatus.ACTIVE);
    }

    @Test
    void enablingMonthlySubscription_initializesNextBillingDateFromStart() {
        Member member = baseMember()
                .subscriptionFrequency(SubscriptionFrequency.MONTHLY)
                .subscriptionAmount(new BigDecimal("25.00"))
                .build();

        memberService.saveMember(member, user(), LocalDate.of(2026, 6, 30));

        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 7, 30));
        assertThat(member.getSubscriptionStatus()).isEqualTo(MemberSubscriptionStatus.ACTIVE);
    }

    @Test
    void enablingYearlySubscription_initializesNextBillingDateFromStart() {
        Member member = baseMember()
                .subscriptionFrequency(SubscriptionFrequency.YEARLY)
                .subscriptionAmount(new BigDecimal("120.00"))
                .build();

        memberService.saveMember(member, user(), LocalDate.of(2026, 6, 30));

        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void nullStartDate_defaultsToToday() {
        Member member = baseMember()
                .subscriptionFrequency(SubscriptionFrequency.MONTHLY)
                .build();

        memberService.saveMember(member, user(), null);

        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void noneFrequency_clearsScheduleAndAmount() {
        Member member = baseMember()
                .subscriptionFrequency(SubscriptionFrequency.NONE)
                .subscriptionAmount(new BigDecimal("25.00"))
                .nextBillingDate(LocalDate.of(2026, 9, 1))
                .build();

        memberService.saveMember(member, user(), LocalDate.of(2026, 6, 30));

        assertThat(member.getSubscriptionFrequency()).isEqualTo(SubscriptionFrequency.NONE);
        assertThat(member.getNextBillingDate()).isNull();
        assertThat(member.getSubscriptionAmount()).isNull();
    }

    @Test
    void existingSchedule_isNotReshuffledOnEdit() {
        LocalDate existing = LocalDate.of(2026, 8, 15);
        Member member = baseMember()
                .id(42L) // existing member
                .subscriptionFrequency(SubscriptionFrequency.MONTHLY)
                .nextBillingDate(existing)
                .build();

        // Editing with a different start date must NOT move the existing billing cycle.
        memberService.saveMember(member, user(), LocalDate.of(2026, 6, 30));

        assertThat(member.getNextBillingDate()).isEqualTo(existing);
    }
}
