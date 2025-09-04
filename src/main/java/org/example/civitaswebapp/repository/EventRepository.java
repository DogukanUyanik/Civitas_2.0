package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartBetween(LocalDateTime start, LocalDateTime end);
    List<Event> findByAttendees_Id(Long memberId); // events a member is attending

    Page<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String search, String search1, Pageable pageable);
}
