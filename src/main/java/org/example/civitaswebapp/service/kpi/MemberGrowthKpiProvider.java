// Member Growth Rate KPI Provider
package org.example.civitaswebapp.service.kpi;

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
                .description("New members this month")
                .icon("trending-up")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newMembersThisMonth = memberRepository.countByCreatedAtAfter(startOfMonth);

        // Calculate percentage growth (you might want to implement this based on your needs)
        double growthPercentage = calculateGrowthPercentage(newMembersThisMonth);

        return KpiValueDto.builder()
                .key("members.growth.month")
                .title("Monthly Growth")
                .value(newMembersThisMonth)
                .unit("%")
                .formattedValue("+" + String.format("%.1f%%", growthPercentage))
                .build();
    }

    private double calculateGrowthPercentage(long newMembers) {
        // Simple calculation - you can make this more sophisticated
        long totalMembers = memberRepository.count();
        if (totalMembers == 0) return 0;
        return ((double) newMembers / totalMembers) * 100;
    }
}