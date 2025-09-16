package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActiveMembersCountKpiProvider implements KpiProvider {


    @Autowired
    private MemberRepository  memberRepository;


    @Override
    public String getKey() {
        return "members.active.count";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Active Members")
                .description("Total number of active members")
                .icon("users")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        long activeCount = memberRepository.countByMemberStatus(MemberStatus.ACTIVE);
        return KpiValueDto.builder()
                .key("members.active.count")
                .title("Active Members")
                .value(activeCount)
                .unit("members")
                .formattedValue(activeCount + " Members")
                .build();
    }
}
