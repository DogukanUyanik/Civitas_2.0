package org.example.civitaswebapp.service;


import org.example.civitaswebapp.domain.DashboardTile;
import org.example.civitaswebapp.domain.MyUser;

import java.util.List;
import java.util.Optional;

public interface DashboardService {
    List<DashboardTile> getTilesForUser(MyUser user);

    Optional<DashboardTile> getTileById(Long id);

    DashboardTile saveTile(DashboardTile dashboardTile);

    void deleteTile(DashboardTile dashboardTile);
}
