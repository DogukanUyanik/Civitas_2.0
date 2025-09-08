package org.example.civitaswebapp.service;

import org.example.civitaswebapp.dto.KPIDataDTO;
import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KPIServiceImpl implements KPIService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nl-BE"));
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("nl-BE"));

    @Override
    public Map<String, KPIDataDTO> getAllKPIData() {
        Map<String, KPIDataDTO> kpiData = new HashMap<>();

        kpiData.put("total-members", getTotalMembers());
        kpiData.put("payments-received", getPaymentsReceived());
        kpiData.put("pending-payments", getPendingPayments());
        kpiData.put("upcoming-events", getUpcomingEvents());
        kpiData.put("recent-transactions", getRecentTransactions());
        kpiData.put("member-status", getMemberStatusBreakdown());

        return kpiData;
    }

    @Override
    public KPIDataDTO getTotalMembers() {
        long totalMembers = memberRepository.count();
        long newMembersThisMonth = getNewMembersThisMonth();

        String trend = newMembersThisMonth > 0 ? "up" : "neutral";
        String subValue = newMembersThisMonth > 0 ?
                "+" + newMembersThisMonth + " this month" :
                "No new members this month";

        return new KPIDataDTO(
                numberFormatter.format(totalMembers),
                subValue,
                trend,
                "#10b981",
                "fas fa-users",
                (double) totalMembers,
                "count"
        );
    }

    @Override
    public KPIDataDTO getPaymentsReceived() {
        // For now using mock data since you don't have Payment entity yet
        // Replace this when you add payment functionality
        BigDecimal totalPayments = getPaymentsThisMonth();
        double growthPercentage = getPaymentGrowthPercentage();

        String trend = growthPercentage > 0 ? "up" : growthPercentage < 0 ? "down" : "neutral";
        String trendText = growthPercentage != 0 ?
                String.format("%+.1f%% from last month", growthPercentage) :
                "No change from last month";

        return new KPIDataDTO(
                currencyFormatter.format(totalPayments),
                trendText,
                trend,
                "#3b82f6",
                "fas fa-money-bill-wave",
                totalPayments.doubleValue(),
                "currency"
        );
    }

    @Override
    public KPIDataDTO getPendingPayments() {
        // Mock implementation - replace when you have payment functionality
        int pendingCount = getPendingPaymentsCount();
        BigDecimal pendingAmount = getPendingPaymentsAmount();

        String trend = pendingCount > 10 ? "up" : pendingCount < 5 ? "down" : "neutral";

        return new KPIDataDTO(
                String.valueOf(pendingCount),
                currencyFormatter.format(pendingAmount) + " total",
                trend,
                "#f59e0b",
                "fas fa-clock",
                (double) pendingCount,
                "count"
        );
    }

    @Override
    public KPIDataDTO getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureLimit = now.plusMonths(3); // Events in next 3 months

        List<Event> upcomingEvents = eventRepository.findByStartBetween(now, futureLimit);
        int upcomingEventsCount = upcomingEvents.size();

        String nextEventName = upcomingEvents.isEmpty() ? null : upcomingEvents.get(0).getTitle();

        return new KPIDataDTO(
                String.valueOf(upcomingEventsCount),
                nextEventName != null ? "Next: " + nextEventName : "No upcoming events",
                "neutral",
                "#8b5cf6",
                "fas fa-calendar-alt",
                (double) upcomingEventsCount,
                "count"
        );
    }

    @Override
    public KPIDataDTO getRecentTransactions() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Transaction> allTransactions = transactionRepository.findAll();

        // Filter transactions from the last 7 days using createdAt field
        BigDecimal recentAmount = allTransactions.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(weekAgo))
                .map(t -> BigDecimal.valueOf(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long transactionCount = allTransactions.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(weekAgo))
                .count();

        int daysPeriod = 7;
        String subValue = transactionCount > 0 ?
                transactionCount + " transactions, last " + daysPeriod + " days" :
                "No transactions, last " + daysPeriod + " days";

        return new KPIDataDTO(
                currencyFormatter.format(recentAmount),
                subValue,
                "neutral",
                "#ef4444",
                "fas fa-exchange-alt",
                recentAmount.doubleValue(),
                "currency"
        );
    }

    @Override
    public KPIDataDTO getMemberStatusBreakdown() {
        long totalMembers = memberRepository.count();
        long activeMembers = memberRepository.countByMemberStatus(MemberStatus.ACTIVE);

        if (totalMembers == 0) {
            return new KPIDataDTO("0%", "No members", "neutral");
        }

        double activePercentage = (activeMembers * 100.0) / totalMembers;
        String trend = activePercentage >= 90 ? "up" : activePercentage < 70 ? "down" : "neutral";

        return new KPIDataDTO(
                Math.round(activePercentage) + "%",
                "Active members (" + activeMembers + "/" + totalMembers + ")",
                trend,
                "#06b6d4",
                "fas fa-chart-pie",
                activePercentage,
                "percentage"
        );
    }

    // Helper methods with real implementations

    private long getNewMembersThisMonth() {
        // Get start of current month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        // Count all members - since Member doesn't seem to have createdDate field,
        // this is a mock implementation. Add createdDate field to Member for real data
        // return memberRepository.countByCreatedDateAfter(startOfMonth);

        // Mock implementation - replace when you add createdDate to Member
        return (long) (Math.random() * 20); // Random number between 0-19
    }

    private BigDecimal getPaymentsThisMonth() {
        // Mock implementation since you don't have Payment entity yet
        // When you add payments, implement like:
        // YearMonth currentMonth = YearMonth.now();
        // LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        // LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        // return paymentRepository.sumAmountByDateRange(startOfMonth, endOfMonth);

        return new BigDecimal("45230.50");
    }

    private double getPaymentGrowthPercentage() {
        // Mock implementation - implement when you have payment data
        return 8.5;
    }

    private int getPendingPaymentsCount() {
        // Mock implementation - implement when you have payment functionality
        return 23;
    }

    private BigDecimal getPendingPaymentsAmount() {
        // Mock implementation - implement when you have payment functionality
        return new BigDecimal("3450.00");
    }
}