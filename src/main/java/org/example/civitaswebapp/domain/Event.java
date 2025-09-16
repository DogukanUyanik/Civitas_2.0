package org.example.civitaswebapp.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @NotBlank(message = "{event.title.required}")
    @Size(max = 100, message = "{event.title.size}")
    private String title;

    @Size(max = 500, message = "{event.description.size}")
    private String description;

    @NotNull(message = "{event.start.required}")
    @Future(message = "{event.start.future}")
    private LocalDateTime start;

    @NotNull(message = "{event.end.required}")
    private LocalDateTime end;

    @Size(max = 100, message = "{event.location.size}")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType = EventType.GENERAL; // Default to GENERAL

    @ManyToMany
    @JsonManagedReference
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
            throw new IllegalArgumentException("{event.end.before.start}");
        }

        if (eventType == null) {
            eventType = EventType.GENERAL;
        }
    }
}
