package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.dto.kpi.RevenueChartDto;
import org.example.civitaswebapp.service.kpi.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/tiles")
    public List<KpiTileDto> getAllTiles() {
        return dashboardService.getAllTiles();
    }

    @GetMapping("/me")
    public List<KpiValueDto> getMyDashboard() {
        return dashboardService.getMyDashboard();
    }

    @GetMapping("/chart/revenue")
    public RevenueChartDto getRevenueChart() {
        return dashboardService.getRevenueChart();
    }

    @PutMapping("/me/tiles")
    public void saveMyDashboard(@RequestBody List<UserDashboardTile> tiles) {
        dashboardService.saveMyDashboard(tiles);
    }

    @PostMapping("/me/tiles/{widgetKey}")
    public void addMyTile(@PathVariable String widgetKey) {
        dashboardService.addMyTile(widgetKey);
    }

    @DeleteMapping("/me/tiles/{widgetKey}")
    public void removeMyTile(@PathVariable String widgetKey) {
        dashboardService.removeMyTile(widgetKey);
    }
}
