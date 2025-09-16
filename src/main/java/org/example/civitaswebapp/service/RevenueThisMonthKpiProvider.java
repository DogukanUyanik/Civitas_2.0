package org.example.civitaswebapp.service;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.param.ChargeListParams;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class RevenueThisMonthKpiProvider implements KpiProvider {

    public RevenueThisMonthKpiProvider(@Value("${stripe.secret.key}") String stripeApiKey) {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public String getKey() {
        return "stripe.totalRevenueMonth";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Revenue This Month")
                .description("Total revenue collected from Stripe this month")
                .icon("fas fa-euro-sign")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate firstDayOfMonth = now.withDayOfMonth(1);

            long startOfMonth = firstDayOfMonth.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long nowTimestamp = Instant.now().getEpochSecond();

            ChargeListParams params = ChargeListParams.builder()
                    .setCreated(ChargeListParams.Created.builder()
                            .setGte(startOfMonth)
                            .setLte(nowTimestamp)
                            .build())
                    .build();

            long totalCents = 0;
            // Auto-pagination iterates through all charges for the period
            for (Charge c : Charge.list(params).autoPagingIterable()) {
                if ("succeeded".equals(c.getStatus())) {
                    totalCents += c.getAmount();
                }
            }

            double totalRevenue = totalCents / 100.0;

            return KpiValueDto.builder()
                    .key(getKey())
                    .title("Revenue This Month")
                    .value(totalRevenue)
                    .unit("€")
                    .formattedValue(String.format("€%.2f", totalRevenue))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return KpiValueDto.builder()
                    .key(getKey())
                    .title("Revenue This Month")
                    .value(0)
                    .unit("€")
                    .formattedValue("€0.00")
                    .build();
        }
    }
}
