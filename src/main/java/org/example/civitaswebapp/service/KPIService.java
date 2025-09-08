package org.example.civitaswebapp.service;

import org.example.civitaswebapp.dto.KPIDataDTO;
import java.util.Map;

public interface KPIService {

    /**
     * Get all KPI data for dashboard tiles
     * @return Map of KPI type to KPI data
     */
    Map<String, KPIDataDTO> getAllKPIData();

    /**
     * Get total number of members
     * @return KPI data for total members
     */
    KPIDataDTO getTotalMembers();

    /**
     * Get payments received data
     * @return KPI data for payments received
     */
    KPIDataDTO getPaymentsReceived();

    /**
     * Get pending payments data
     * @return KPI data for pending payments
     */
    KPIDataDTO getPendingPayments();

    /**
     * Get upcoming events data
     * @return KPI data for upcoming events
     */
    KPIDataDTO getUpcomingEvents();

    /**
     * Get recent transactions data
     * @return KPI data for recent transactions
     */
    KPIDataDTO getRecentTransactions();

    /**
     * Get member status breakdown (active vs inactive)
     * @return KPI data for member status
     */
    KPIDataDTO getMemberStatusBreakdown();
}