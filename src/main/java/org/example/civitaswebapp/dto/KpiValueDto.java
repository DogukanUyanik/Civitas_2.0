package org.example.civitaswebapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KpiValueDto {
    private Object value;    // could be number, string, object depending on the tile
    private String unit;     // optional unit (e.g., "members", "€", "%")
    private String formattedValue; // optional pre-formatted string for display
}
