package org.example.civitaswebapp.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScannedInvoiceDto(
        String filename,
        LocalDate guessedDate,
        BigDecimal guessedAmount,
        String guessedCounterparty
)
{}
