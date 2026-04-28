package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
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
                .description("Total number of members in your union")
                .icon("users")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Union union) {
        long totalCount = memberRepository.countByUnion(union);
        return KpiValueDto.builder()
                .key(getKey())
                .title("Total Members")
                .value(totalCount)
                .unit("members")
                .formattedValue(String.valueOf(totalCount))
                .build();
    }
}
