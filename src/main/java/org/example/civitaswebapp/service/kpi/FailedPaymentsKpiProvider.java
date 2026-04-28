package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.FailedPaymentStatsDto;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
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
                .title("Failed Payments")
                .description("Failed payment breakdown for your union")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Union union) {
        long total = transactionRepository.countByStatusAndUnion(TransactionStatus.FAILED, union);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfLastWeek = startOfThisWeek.minusWeeks(1);

        long thisWeek = transactionRepository.countByStatusAndUnionAndCreatedAtBetween(
                TransactionStatus.FAILED, union, startOfThisWeek, now);
        long lastWeek = transactionRepository.countByStatusAndUnionAndCreatedAtBetween(
                TransactionStatus.FAILED, union, startOfLastWeek, startOfThisWeek);

        return KpiValueDto.builder()
                .key(getKey())
                .title("Failed Payments")
                .type("summary")
                .value(new FailedPaymentStatsDto(total, thisWeek, lastWeek))
                .formattedValue(String.valueOf(total))
                .build();
    }
}
