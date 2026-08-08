package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        MyUser user = user();

        // The edit form does not render nextBillingDate, so the bound entity never carries it. The
        // schedule has to be read back from the persisted row — which is the only reason this
        // guarantee can hold at all.
        Member persisted = baseMember()
                .id(42L)
                .subscriptionFrequency(SubscriptionFrequency.MONTHLY)
                .nextBillingDate(existing)
                .union(user.getUnion())
                .build();
        when(myUserService.getLoggedInUser()).thenReturn(user);
        when(memberRepository.findById(42L)).thenReturn(Optional.of(persisted));

        Member formSubmission = baseMember()
                .id(42L)
                .subscriptionFrequency(SubscriptionFrequency.MONTHLY)
                .build();

        // Editing with a different start date must NOT move the existing billing cycle.
        memberService.saveMember(formSubmission, user, LocalDate.of(2026, 6, 30));

        assertThat(persisted.getNextBillingDate()).isEqualTo(existing);
    }

    @Test
    void editing_preservesFieldsTheFormDoesNotRender() {
        // Saving the detached form entity wholesale used to null every unrendered column: the
        // member's last payment date vanished and their language silently reverted to NL.
        LocalDate lastPayment = LocalDate.of(2026, 5, 1);
        MyUser user = user();

        Member persisted = baseMember()
                .id(42L)
                .dateOfLastPayment(lastPayment)
                .subscriptionStatus(MemberSubscriptionStatus.PAUSED)
                .union(user.getUnion())
                .build();
        when(myUserService.getLoggedInUser()).thenReturn(user);
        when(memberRepository.findById(42L)).thenReturn(Optional.of(persisted));

        Member formSubmission = baseMember().id(42L).address("2 New St").build();

        memberService.saveMember(formSubmission, user, null);

        assertThat(persisted.getAddress()).isEqualTo("2 New St");
        assertThat(persisted.getDateOfLastPayment()).isEqualTo(lastPayment);
        assertThat(persisted.getSubscriptionStatus()).isEqualTo(MemberSubscriptionStatus.PAUSED);
    }

    @Test
    void editing_appliesTheSelectedLanguageToThePersistedMember() {
        MyUser user = user();
        Member persisted = baseMember().id(42L).language(MemberLanguage.NL).union(user.getUnion()).build();
        when(myUserService.getLoggedInUser()).thenReturn(user);
        when(memberRepository.findById(42L)).thenReturn(Optional.of(persisted));

        Member formSubmission = baseMember().id(42L).language(MemberLanguage.TR).build();

        memberService.saveMember(formSubmission, user, null);

        assertThat(persisted.getLanguage()).isEqualTo(MemberLanguage.TR);
    }
}
