package org.example.civitaswebapp.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression guard for the {@code ConcurrentModificationException} in
 * {@code PersistentSet.injectLoadedState}.
 *
 * <p>Root cause: Lombok {@code @Data} generated {@code equals}/{@code hashCode}/{@code toString} over
 * the bidirectional {@code Event.attendees} ↔ {@code Member.events} collections. During Hibernate's
 * own collection load, {@code set.add(...)} calls {@code hashCode()}, which walked the other side's
 * collection, lazily initializing it reentrantly and corrupting the loading state. These tests fail
 * (StackOverflowError / hashCode instability) if anyone reintroduces collection-based identity.
 */
class EntityIdentityTest {

    private Member member(Long id) {
        return Member.builder().id(id).firstName("A").lastName("B").email("a@b.c").build();
    }

    private Event event(Long id) {
        Event e = new Event();
        e.setId(id);
        e.setTitle("Kickoff");
        return e;
    }

    @Test
    void eventHashCodeIsIndependentOfAttendees() {
        Event e = event(1L);
        int empty = e.hashCode();

        e.setAttendees(new HashSet<>(Set.of(member(10L), member(11L))));
        int withAttendees = e.hashCode();

        assertThat(withAttendees).isEqualTo(empty);
    }

    @Test
    void memberHashCodeIsIndependentOfEvents() {
        Member m = member(10L);
        int empty = m.hashCode();

        m.setEvents(new HashSet<>(Set.of(event(1L), event(2L))));
        int withEvents = m.hashCode();

        assertThat(withEvents).isEqualTo(empty);
    }

    @Test
    void identityIsBasedOnIdOnly() {
        assertThat(event(1L)).isEqualTo(event(1L));
        assertThat(event(1L)).isNotEqualTo(event(2L));
        assertThat(member(5L)).isEqualTo(member(5L));
        assertThat(member(5L)).isNotEqualTo(member(6L));
    }

    @Test
    void bidirectionalCycleDoesNotRecurseInHashCodeEqualsOrToString() {
        Event e = event(1L);
        Member m = member(10L);

        // Wire the cycle exactly like Hibernate would after loading the join table.
        e.setAttendees(new HashSet<>(Set.of(m)));
        m.setEvents(new HashSet<>(Set.of(e)));

        // With collection-based @Data identity these StackOverflow; with id-only identity they return.
        assertThatCode(() -> {
            e.hashCode();
            e.toString();
            e.equals(event(1L));
            m.hashCode();
            m.toString();
            m.equals(member(10L));
        }).doesNotThrowAnyException();
    }
}
