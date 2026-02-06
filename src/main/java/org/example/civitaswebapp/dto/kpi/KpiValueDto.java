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
    private String key;              // unique identifier for the KPI
    private String title;            // human-readable title
    private Object value;            // raw value (e.g. 20)
    private String unit;             // optional (e.g. "members", "€", "%")
    private String formattedValue;   // pre-formatted string for display
}
