package org.example.civitaswebapp.dto.transactions;

import java.time.LocalDateTime;

public record TransactionCreatedDto(
        Long id,
        Long memberId,
        String firstName,
        String lastName,
        Double amount,
        LocalDateTime createdAt,
        Long createdByUserId) {
}
