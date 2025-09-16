package org.example.civitaswebapp.service;

import org.example.civitaswebapp.dto.KpiTileDto;
import org.example.civitaswebapp.dto.KpiValueDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.domain.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UpcomingEventsKpiProvider implements KpiProvider {

    @Autowired
    private final EventRepository eventRepository;

    public UpcomingEventsKpiProvider(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public String getKey() {
        return "events.upcoming";
    }

    @Override
    public KpiTileDto getTileMetadata() {
        return KpiTileDto.builder()
                .key(getKey())
                .title("Upcoming Events")
                .description("Upcoming events in the next 30 days")
                .defaultEnabled(true)
                .build();
    }

    @Override
    public KpiValueDto computeValue(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Days = now.plusDays(30);

        List<Event> upcomingEvents = eventRepository.findByStartBetween(now, in30Days);

        // Convert to a simple list of event info to send to frontend
        List<String> eventTitles = upcomingEvents.stream()
                .map(event -> event.getStart() + " - " + event.getTitle())
                .collect(Collectors.toList());

        return KpiValueDto.builder()
                .key(getKey())
                .title("Upcoming Events")
                .value(eventTitles) // store list of events
                .formattedValue(String.join("\n", eventTitles)) // simple string for display
                .build();
    }
}
