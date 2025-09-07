package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Start date/time is required")
    @Future(message = "Start date/time must be in the future")
    private LocalDateTime start;

    @NotNull(message = "End date/time is required")
    private LocalDateTime end;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType = EventType.GENERAL; // Default to GENERAL

    @ManyToMany
    @JoinTable(
            name = "event_members",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<Member> attendees = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void validateDates() {
        if (end != null && start != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End date/time cannot be before start date/time");
        }

        // Set default event type if null
        if (eventType == null) {
            eventType = EventType.GENERAL;
        }
    }
}