package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;

import java.util.List;

public interface DashboardService {

    // Get all available KPI tiles metadata
    List<KpiTileDto> getAllTiles();

    // Get computed KPI values for a specific user
    List<KpiValueDto> getUserDashboard(Long userId);

    // Save or update user tile preferences
    void saveUserDashboard(Long userId, List<UserDashboardTile> tiles);

}
