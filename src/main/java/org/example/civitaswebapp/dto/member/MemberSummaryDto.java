package org.example.civitaswebapp.dto.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record MemberSummaryDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String dateOfLastPayment
) {}
