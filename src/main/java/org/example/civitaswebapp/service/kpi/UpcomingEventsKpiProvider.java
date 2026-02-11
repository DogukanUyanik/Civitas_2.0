package org.example.civitaswebapp.service.kpi;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.kpi.KpiTileDto;
import org.example.civitaswebapp.dto.kpi.KpiValueDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // 👈 Import this
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UpcomingEventsKpiProvider implements KpiProvider {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MyUserRepository myUserRepository;

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
        MyUser user = myUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for KPI calculation"));

        Union currentUnion = user.getUnion();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Days = now.plusDays(30);

        List<Event> upcomingEvents = eventRepository.findByStartBetweenAndUnion(now, in30Days, currentUnion);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm");

        List<String> eventTitles = upcomingEvents.stream()
                .map(event -> {
                    String formattedDate = event.getStart().format(formatter);
                    return formattedDate + " - " + event.getTitle();
                })
                .collect(Collectors.toList());

        return KpiValueDto.builder()
                .key(getKey())
                .title("Upcoming Events")
                .value(eventTitles)
                .formattedValue(String.join("\n", eventTitles))
                .build();
    }
}