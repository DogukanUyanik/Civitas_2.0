package org.example.civitaswebapp.service.kpi;

import lombok.RequiredArgsConstructor;
import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UpcomingEventsKpiProvider implements KpiProvider {

    private static final Logger log = LoggerFactory.getLogger(UpcomingEventsKpiProvider.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private final EventRepository eventRepository;

    @Override
    public String getKey() {
        return "events.upcoming";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Upcoming Events")
                .description("Events starting within the next 30 days")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Union union) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Days = now.plusDays(30);

        List<Event> upcomingEvents = eventRepository.findByStartBetweenAndUnion(now, in30Days, union);
        log.debug("Found {} upcoming events for union {}", upcomingEvents.size(), union.getId());

        List<String> eventTitles = upcomingEvents.stream()
                .map(e -> e.getStart().format(FORMATTER) + " - " + e.getTitle())
                .collect(Collectors.toList());

        return KpiValueDto.builder()
                .key(getKey())
                .title("Upcoming Events")
                .type("list")
                .value(eventTitles)
                .formattedValue(String.join("\n", eventTitles))
                .build();
    }
}
