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

        // If user has no custom tiles, return all available KPIs (standard dashboard)
        if (userTiles.isEmpty()) {
            return getStandardDashboard(userId);
        }

        // Only compute KPIs the user has enabled (for users with custom config)
        List<KpiValueDto> values = new ArrayList<>();
        for (UserDashboardTile tile : userTiles) {
            String widgetKey = tile.getWidgetKey();

            kpiProviders.stream()
                    .filter(p -> p.getTileMetadata().getKey().equals(widgetKey))
                    .findFirst()
                    .ifPresent(provider -> {
                        KpiValueDto value = provider.computeValue(userId);
                        values.add(value);
                    });
        }

        return values;
    }

    private List<KpiValueDto> getStandardDashboard(Long userId) {
        // Return all available KPIs for the standard dashboard
        List<KpiValueDto> values = new ArrayList<>();

        for (KpiProvider provider : kpiProviders) {
            try {
                KpiValueDto value = provider.computeValue(userId);
                values.add(value);
            } catch (Exception e) {
                // Log the error but continue with other KPIs
                System.err.println("Error computing KPI " + provider.getTileMetadata().getKey() + ": " + e.getMessage());
            }
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

    @Override
    public void addTile(Long userId, String widgetKey) {
        boolean exists = tileRepository.existsByUserIdAndWidgetKey(userId, widgetKey);

        if (!exists) {
            int position = getNextPosition(userId);

            UserDashboardTile tile = UserDashboardTile.builder()
                    .widgetKey(widgetKey)
                    .enabled(true)
                    .position(position)
                    .user(MyUser.builder().id(userId).build()) // reference user only by id
                    .build();

            tileRepository.save(tile);
        }
    }


    public void removeTile(Long userId, String key) {
        tileRepository.deleteByUserIdAndWidgetKey(userId, key);
    }

    private int getNextPosition(Long userId) {
        List<UserDashboardTile> tiles = tileRepository.findByUserIdOrderByPositionAsc(userId);
        if (tiles.isEmpty()) {
            return 0; // first tile
        }
        return tiles.get(tiles.size() - 1).getPosition() + 1; // last position + 1
    }



}
