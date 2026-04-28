package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MemberGrowthKpiProvider implements KpiProvider {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public String getKey() {
        return "members.growth.month";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Monthly Growth")
                .description("New members added this month")
                .icon("trending-up")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Union union) {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long newMembersThisMonth = memberRepository.countByCreatedAtAfterAndUnion(startOfMonth, union);

        return KpiValueDto.builder()
                .key(getKey())
                .title("Monthly Growth")
                .value(newMembersThisMonth)
                .unit("new")
                .formattedValue("+" + newMembersThisMonth)
                .build();
    }
}
