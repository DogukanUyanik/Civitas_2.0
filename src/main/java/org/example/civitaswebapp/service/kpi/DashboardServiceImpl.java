package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.domain.UserDashboardTile;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.dto.kpi.RevenueChartDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.example.civitaswebapp.repository.UserDashboardTileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private List<KpiProvider> kpiProviders;

    @Autowired
    private UserDashboardTileRepository tileRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private MyUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MyUser user) return user;
        throw new RuntimeException("No authenticated user in security context");
    }

    @Override
    public List<KpiTileDto> getAllTiles() {
        return kpiProviders.stream()
                .map(KpiProvider::getTileMetadata)
                .collect(Collectors.toList());
    }

    @Override
    public List<KpiValueDto> getMyDashboard() {
        MyUser user = getCurrentUser();
        Union union = user.getUnion();

        List<UserDashboardTile> userTiles = tileRepository.findByUserIdOrderByPositionAsc(user.getId());

        if (userTiles.isEmpty()) {
            return computeAllKpis(union);
        }

        return userTiles.stream()
                .flatMap(tile -> kpiProviders.stream()
                        .filter(p -> p.getKey().equals(tile.getWidgetKey()))
                        .map(p -> safeCompute(p, union)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public RevenueChartDto getRevenueChart() {
        Union union = getCurrentUser().getUnion();
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();

        List<Object[]> rows = transactionRepository.revenueByDayInPeriod(union, TransactionStatus.SUCCEEDED, start, end);

        Map<Integer, Double> dayRevenue = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int day = ((Number) row[0]).intValue();
            double revenue = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            dayRevenue.put(day, revenue);
        }

        int today = LocalDate.now().getDayOfMonth();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int day = 1; day <= today; day++) {
            labels.add(String.valueOf(day));
            data.add(dayRevenue.getOrDefault(day, 0.0));
        }

        return new RevenueChartDto(labels, data);
    }

    @Override
    public void saveMyDashboard(List<UserDashboardTile> tiles) {
        MyUser user = getCurrentUser();
        for (UserDashboardTile tile : tiles) {
            tileRepository.deleteByUserIdAndWidgetKey(user.getId(), tile.getWidgetKey());
            tile.setUser(user);
            tileRepository.save(tile);
        }
    }

    @Override
    public void addMyTile(String widgetKey) {
        MyUser user = getCurrentUser();
        if (!tileRepository.existsByUserIdAndWidgetKey(user.getId(), widgetKey)) {
            List<UserDashboardTile> existing = tileRepository.findByUserIdOrderByPositionAsc(user.getId());
            int position = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getPosition() + 1;
            tileRepository.save(UserDashboardTile.builder()
                    .widgetKey(widgetKey)
                    .enabled(true)
                    .position(position)
                    .user(user)
                    .build());
        }
    }

    @Override
    public void removeMyTile(String widgetKey) {
        tileRepository.deleteByUserIdAndWidgetKey(getCurrentUser().getId(), widgetKey);
    }

    private List<KpiValueDto> computeAllKpis(Union union) {
        return kpiProviders.stream()
                .map(p -> safeCompute(p, union))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private KpiValueDto safeCompute(KpiProvider provider, Union union) {
        try {
            return provider.computeValue(union);
        } catch (Exception e) {
            log.error("KPI computation failed for '{}': {}", provider.getKey(), e.getMessage(), e);
            return null;
        }
    }
}
