package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_dashboard_tile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDashboardTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MyUser user; // link to your MyUser entity

    @Column(nullable = false)
    private String widgetKey;

    private boolean enabled = true;

    private int position = 0;

    @Column(columnDefinition = "text")
    private String settingsJson; // optional per-tile settings
}
