package org.example.civitaswebapp.dto.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KpiValueDto {
    private String key;
    private String title;
    private Object value;
    private String unit;
    private String formattedValue;
    /** Discriminates the shape of {@code value}: "scalar" | "list" | "summary" */
    @Builder.Default
    private String type = "scalar";
}
