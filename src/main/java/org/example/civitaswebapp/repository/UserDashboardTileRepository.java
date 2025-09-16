package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.UserDashboardTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserDashboardTileRepository extends JpaRepository<UserDashboardTile, Long> {

    // Find tiles by user ID
    List<UserDashboardTile> findByUserIdOrderByPositionAsc(Long userId);

    // Delete tile by user ID and widget key
    @Transactional
    @Modifying
    void deleteByUserIdAndWidgetKey(Long userId, String widgetKey);

    boolean existsByUserIdAndWidgetKey(Long userId, String widgetKey);

}
