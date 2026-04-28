package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class RevenueThisMonthKpiProvider implements KpiProvider {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public String getKey() {
        return "revenue.this.month";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Revenue This Month")
                .description("Total revenue from succeeded transactions this month")
                .icon("fas fa-euro-sign")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Union union) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Double totalRevenue = transactionRepository.sumAmountByUnionAndStatusAndPeriod(
                union, TransactionStatus.SUCCEEDED, startOfMonth, now);

        if (totalRevenue == null) totalRevenue = 0.0;

        return KpiValueDto.builder()
                .key(getKey())
                .title("Revenue This Month")
                .value(totalRevenue)
                .unit("€")
                .formattedValue(String.format("€%.2f", totalRevenue))
                .build();
    }
}
