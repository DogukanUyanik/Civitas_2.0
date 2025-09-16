package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.dto.TransactionSummaryDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FailedPaymentsKpiProvider implements KpiProvider {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public String getKey() {
        return "transactions.failed";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Failed payments")
                .description("Failed payments")
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        List<Transaction> failedTransactions = transactionRepository.findAllByStatus(TransactionStatus.FAILED);

        // Map transactions to summary DTOs
        List<TransactionSummaryDto> summaries = failedTransactions.stream()
                .map(t -> new TransactionSummaryDto(
                        t.getId(),
                        t.getMember().getName(), // member name
                        t.getAmount(),
                        t.getCurrency(),
                        t.getStatus().name(),
                        t.getCreatedAt()
                ))
                .toList();

        // Format display string with count
        String formattedValue = "Failed: " + failedTransactions.size();

        return KpiValueDto.builder()
                .key(getKey())
                .title("Failed Transactions")
                .value(summaries)       // full list
                .formattedValue(formattedValue) // simple count for the card
                .build();
    }



}
