// Total Members KPI Provider
package org.example.civitaswebapp.service;

import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TotalMembersKpiProvider implements KpiProvider {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public String getKey() {
        return "members.total.count";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Total Members")
                .description("Total number of members in the system")
                .icon("users")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        long totalCount = memberRepository.count();
        return KpiValueDto.builder()
                .key("members.total.count")
                .title("Total Members")
                .value(totalCount)
                .unit("members")
                .formattedValue(String.valueOf(totalCount))
                .build();
    }
}