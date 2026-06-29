package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
// id-only equals/hashCode/toString. NEVER include the bidirectional collections (attendees) or
// associations: Lombok's default @Data walks every field, so hashCode()/toString() on Event would
// recurse through attendees -> Member -> events -> Event..., lazily initializing collections during
// Hibernate's own collection load and throwing ConcurrentModificationException in injectLoadedState.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "{event.title.required}")
    @Size(max = 100, message = "{event.title.size}")
    @ToString.Include
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
    @JoinTable(
            name = "event_members",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<Member> attendees = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "union_id", nullable = false)
    private Union union;

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
