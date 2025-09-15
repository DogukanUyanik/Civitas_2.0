package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.UserDashboardTileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private List<KpiProvider> kpiProviders; // all KPI providers registered as Spring beans

    @Autowired
    private UserDashboardTileRepository tileRepository;

    @Autowired
    private MyUserRepository userRepository;

    @Override
    public List<KpiTileDto> getAllTiles() {
        return kpiProviders.stream()
                .map(KpiProvider::getTileMetadata)
                .collect(Collectors.toList());
    }

    @Override
    public List<KpiValueDto> getUserDashboard(Long userId) {
        // Load user-specific tile config
        List<UserDashboardTile> userTiles = tileRepository.findByUserIdOrderByPositionAsc(userId);

        // Compute all KPI values
        List<KpiValueDto> values = new ArrayList<>();
        for (KpiProvider provider : kpiProviders) {
            KpiValueDto value = provider.computeValue(userId);
            values.add(value);
        }
        return values;
    }

    @Override
    public void saveUserDashboard(Long userId, List<UserDashboardTile> tiles) {
        // Fetch the MyUser entity
        MyUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        // Delete existing tiles and save new ones
        for (UserDashboardTile tile : tiles) {
            tileRepository.deleteByUserIdAndWidgetKey(userId, tile.getWidgetKey());
            tile.setUser(user); // set the entity, not the ID
            tileRepository.save(tile);
        }
    }

}
