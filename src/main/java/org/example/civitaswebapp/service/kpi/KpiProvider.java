package org.example.civitaswebapp.service.kpi;


import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;

public interface KpiProvider {

    /**
     * Unique key for this KPI tile
     */
    String getKey();

    /**
     * Metadata about the tile (title, description, icon)
     */
    KpiTileDto getTileMetadata();

    /**
     * Compute the KPI value for the given user
     * @param userId the user requesting the dashboard
     * @return computed value
     */
    KpiValueDto computeValue(Long userId);
}