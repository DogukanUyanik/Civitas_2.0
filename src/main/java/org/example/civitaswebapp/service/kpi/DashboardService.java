package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.dto.kpi.RevenueChartDto;

import java.util.List;

public interface DashboardService {

    List<KpiTileDto> getAllTiles();

    List<KpiValueDto> getMyDashboard();

    RevenueChartDto getRevenueChart();

    void saveMyDashboard(List<UserDashboardTile> tiles);

    void addMyTile(String widgetKey);

    void removeMyTile(String widgetKey);
}
