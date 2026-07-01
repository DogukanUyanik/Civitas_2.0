package org.example.civitaswebapp.dto.kpi;

import java.util.List;

public record RevenueChartDto(List<String> labels, List<Double> data) {}
