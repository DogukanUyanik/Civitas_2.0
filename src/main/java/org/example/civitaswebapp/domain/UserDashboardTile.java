package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "user_dashboard_tile")
@Data
// id-only identity: never let equals/hashCode/toString walk the user association — see
// [[lombok-data-jpa-entities]].
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDashboardTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
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
