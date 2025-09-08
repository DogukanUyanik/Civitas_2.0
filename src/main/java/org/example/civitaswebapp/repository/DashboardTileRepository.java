package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.DashboardTile;
import org.example.civitaswebapp.domain.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardTileRepository extends JpaRepository<DashboardTile, Long> {

    List<DashboardTile> findByUser(MyUser user);
}
