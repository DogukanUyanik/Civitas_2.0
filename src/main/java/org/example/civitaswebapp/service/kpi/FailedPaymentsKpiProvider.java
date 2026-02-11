package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.dto.transactions.TransactionSummaryDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FailedPaymentsKpiProvider implements KpiProvider {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MyUserRepository myUserRepository;

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
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {

        MyUser user = myUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for KPI calculation"));

        Union currentUnion = user.getUnion();

        List<Transaction> failedTransactions = transactionRepository.findAllByStatusAndUnion(TransactionStatus.FAILED, currentUnion);

        List<TransactionSummaryDto> summaries = failedTransactions.stream()
                .map(t -> new TransactionSummaryDto(
                        t.getId(),
                        t.getMember().getName(),
                        t.getAmount(),
                        t.getCurrency(),
                        t.getStatus().name(),
                        t.getCreatedAt()
                ))
                .toList();

        String formattedValue = "Failed: " + failedTransactions.size();

        return KpiValueDto.builder()
                .key(getKey())
                .title("Failed Transactions")
                .value(summaries)
                .formattedValue(formattedValue)
                .build();
    }



}
