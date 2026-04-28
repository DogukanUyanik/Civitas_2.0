package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
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
    public KpiValueDto computeValue(Union union) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long overdueCount = memberRepository.countByDateOfLastPaymentBeforeAndUnion(thirtyDaysAgo, union);
        return KpiValueDto.builder()
                .key(getKey())
                .title("Members with Overdue Payments")
                .value(overdueCount)
                .formattedValue(overdueCount + " overdue")
                .build();
    }
}
