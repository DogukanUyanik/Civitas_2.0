package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;

public interface KpiProvider {

    String getKey();

    KpiTileDto getTileMetadata();

    KpiValueDto computeValue(Union union);
}
