package org.example.civitaswebapp.dto.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KpiTileDto {
    private String key;
    private String title;
    private String description;
    private String icon;
    @Builder.Default
    private boolean defaultEnabled = true;
}
