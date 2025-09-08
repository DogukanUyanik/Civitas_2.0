package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.DashboardTile;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.KPIDataDTO;
import org.example.civitaswebapp.service.DashboardService;
import org.example.civitaswebapp.service.KPIService;
import org.example.civitaswebapp.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard-tiles")
public class DashboardTileRestController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private KPIService kpiService;

    // Fetch all tiles for the logged-in user
    @GetMapping
    public ResponseEntity<List<DashboardTile>> getTilesForUser() {
        MyUser currentUser = myUserService.getLoggedInUser();
        System.out.println("Fetching tiles for user: " + currentUser.getUsername());
        List<DashboardTile> tiles = dashboardService.getTilesForUser(currentUser);
        return ResponseEntity.ok(tiles);
    }

    // Create or update a tile
    @PostMapping
    public ResponseEntity<DashboardTile> saveTile(@RequestBody DashboardTile tile) {
        MyUser currentUser = myUserService.getLoggedInUser();
        tile.setUser(currentUser); // Make sure the tile is linked to this user
        DashboardTile savedTile = dashboardService.saveTile(tile);
        return ResponseEntity.ok(savedTile);
    }

    // Delete a tile
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTile(@PathVariable Long id) {
        return dashboardService.getTileById(id)
                .map(tile -> {
                    dashboardService.deleteTile(tile);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // NEW: Get KPI data for all tiles
    @GetMapping("/kpi-data")
    public ResponseEntity<Map<String, KPIDataDTO>> getKPIData() {
        Map<String, KPIDataDTO> kpiData = kpiService.getAllKPIData();
        return ResponseEntity.ok(kpiData);
    }

    // NEW: Get specific KPI data by type
    @GetMapping("/kpi-data/{type}")
    public ResponseEntity<KPIDataDTO> getSpecificKPIData(@PathVariable String type) {
        KPIDataDTO kpiData;

        switch (type) {
            case "total-members":
                kpiData = kpiService.getTotalMembers();
                break;
            case "payments-received":
                kpiData = kpiService.getPaymentsReceived();
                break;
            case "pending-payments":
                kpiData = kpiService.getPendingPayments();
                break;
            case "upcoming-events":
                kpiData = kpiService.getUpcomingEvents();
                break;
            case "recent-transactions":
                kpiData = kpiService.getRecentTransactions();
                break;
            case "member-status":
                kpiData = kpiService.getMemberStatusBreakdown();
                break;
            default:
                return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(kpiData);
    }
}