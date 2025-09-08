package org.example.civitaswebapp.service;


import org.example.civitaswebapp.domain.DashboardTile;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.repository.DashboardTileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardTileRepository dashboardTileRepository;

    @Override
    public List<DashboardTile> getTilesForUser(MyUser user) {
        return dashboardTileRepository.findByUser(user);
    }

    @Override
    public Optional<DashboardTile> getTileById(Long id) {
        return dashboardTileRepository.findById(id);
    }

    @Override
    public DashboardTile saveTile(DashboardTile dashboardTile) {
        return dashboardTileRepository.save(dashboardTile);
    }

    @Override
    public void deleteTile(DashboardTile dashboardTile) {
        dashboardTileRepository.delete(dashboardTile);

    }
}
