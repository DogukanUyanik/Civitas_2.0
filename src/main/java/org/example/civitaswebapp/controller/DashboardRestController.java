package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {

    @Autowired
    private DashboardService dashboardService;

    // Get all available KPI tiles (metadata)
    @GetMapping("/tiles")
    public List<KpiTileDto> getAllTiles() {
        return dashboardService.getAllTiles();
    }

    // Get computed KPI values for a user
    @GetMapping("/users/{userId}")
    public List<KpiValueDto> getUserDashboard(@PathVariable Long userId) {
        return dashboardService.getUserDashboard(userId);
    }

    // Save/update user's dashboard tile preferences
    @PutMapping("/users/{userId}")
    public void saveUserDashboard(@PathVariable Long userId, @RequestBody List<UserDashboardTile> tiles) {
        dashboardService.saveUserDashboard(userId, tiles);
    }

    @PostMapping("/users/{userId}/tiles/{widgetKey}")
    public void addTile(@PathVariable Long userId, @PathVariable String widgetKey) {
        dashboardService.addTile(userId, widgetKey);
    }

    @DeleteMapping("/users/{userId}/tiles/{widgetKey}")
    public void removeTile(@PathVariable Long userId, @PathVariable String widgetKey) {
        dashboardService.removeTile(userId, widgetKey);
    }
}
