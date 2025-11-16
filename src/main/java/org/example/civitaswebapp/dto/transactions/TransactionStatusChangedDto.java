package org.example.civitaswebapp.dto.transactions;

import org.example.civitaswebapp.domain.TransactionStatus;

public record TransactionStatusChangedDto(
        Long id,
        Long memberId,
        TransactionStatus oldStatus,
        TransactionStatus newStatus
) {
}
