package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;

import java.util.List;

public interface DashboardService {

    List<KpiTileDto> getAllTiles();

    List<KpiValueDto> getUserDashboard(Long userId);

    void saveUserDashboard(Long userId, List<UserDashboardTile> tiles);

    void addTile(Long userId, String widgetKey);

    void removeTile(Long userId, String widgetKey);

}
