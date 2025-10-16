package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.dto.member.MemberSummaryDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MembersWithOverduePaymentsKpiProvider implements KpiProvider {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public String getKey() {
        return "members.overduePayments";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Members with Overdue Payments")
                .description("Members whose last payment date is more than 30 days ago")
                .icon("fas fa-user-clock")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        List<Member> overdueMembers = memberRepository.findByDateOfLastPaymentBefore(thirtyDaysAgo);

        List<MemberSummaryDto> memberSummaries = overdueMembers.stream()
                .map(m -> new MemberSummaryDto(
                        m.getId(),
                        m.getName(),
                        m.getEmail(),
                        m.getPhoneNumber(),
                        m.getDateOfLastPayment() != null ? m.getDateOfLastPayment().toString() : "N/A"
                ))
                .toList();

        String formattedValue = memberSummaries.size() + " overdue";

        return KpiValueDto.builder()
                .key(getKey())
                .title("Members with Overdue Payments")
                .value(memberSummaries) // safe DTO list
                .formattedValue(formattedValue)
                .build();
    }

}
