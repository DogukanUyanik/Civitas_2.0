package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    
    List<Event> findAllByUnion(Union union);
    Page<Event> findAllByUnion(Union union, Pageable pageable);

    List<Event> findByStartBetweenAndUnion(LocalDateTime startAfter, LocalDateTime startBefore, Union union);

    List<Event> findByAttendees_IdAndUnion(Long attendeesId, Union union);

    Optional<Event> findByIdAndUnion(Long id, Union union);

    @Query("SELECT e FROM Event e WHERE e.union = :union AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchByUnion(
            @Param("union") Union union,
            @Param("search") String search,
            Pageable pageable);

}
