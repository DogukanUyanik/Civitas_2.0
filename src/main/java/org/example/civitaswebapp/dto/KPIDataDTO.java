package org.example.civitaswebapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KPIDataDTO {

    private String value;
    private String subValue;
    private String trend; // "up", "down", "neutral"
    private String color; // hex color code for styling
    private String icon; // FontAwesome icon class
    private Double numericValue; // for calculations and comparisons
    private String unit; // currency, percentage, count, etc.

    // Convenience constructors
    public KPIDataDTO(String value, String subValue) {
        this.value = value;
        this.subValue = subValue;
        this.trend = "neutral";
    }

    public KPIDataDTO(String value, String subValue, String trend) {
        this.value = value;
        this.subValue = subValue;
        this.trend = trend;
    }

    // Helper methods
    public boolean isPositiveTrend() {
        return "up".equals(trend);
    }

    public boolean isNegativeTrend() {
        return "down".equals(trend);
    }

    public String getTrendIcon() {
        switch (trend) {
            case "up":
                return "fas fa-arrow-up";
            case "down":
                return "fas fa-arrow-down";
            default:
                return "fas fa-minus";
        }
    }

    public String getTrendColor() {
        switch (trend) {
            case "up":
                return "#10b981";
            case "down":
                return "#ef4444";
            default:
                return "#6b7280";
        }
    }
}