package org.example.civitaswebapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KpiTileDto {
    private String key;          // unique key for the tile (e.g., "members.active.count")
    private String title;        // tile title
    private String description;  // short description
    private String icon;         // optional icon name
    private boolean defaultEnabled = true; // whether tile is shown by default
}
