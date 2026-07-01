package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guards the thread-isolation fix for the {@code ConcurrentModificationException} in
 * {@code PersistentSet.injectLoadedState}. The async listener must drive notifications purely from
 * the immutable {@link EventSavedEventDto} and never reload the {@code Event} or touch its
 * {@code attendees} collection — otherwise the request thread and the async thread race on the same
 * Hibernate {@code PersistentSet}.
 */
@ExtendWith(MockitoExtension.class)
class EventNotificationListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private MyUserRepository myUserRepository;
    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private EventNotificationListener listener;

    private EventSavedEventDto dtoWithAttendees(List<String> phones) {
        return new EventSavedEventDto(
                7L,
                "Kickoff",
                "First meetup",
                LocalDateTime.of(2026, 7, 1, 18, 0),
                LocalDateTime.of(2026, 7, 1, 20, 0),
                "Ghent",
                "GENERAL",
                1L,
                true,
                phones
        );
    }

    @Test
    void handleEventSaved_messagesEachAttendeeFromDto_withoutLoadingTheEvent() {
        when(myUserRepository.findById(1L)).thenReturn(Optional.of(new MyUser()));
        EventSavedEventDto dto = dtoWithAttendees(List.of("+32470000001", "+32470000002"));

        listener.handleEventSaved(dto);

        // One notification for the actor — now carries i18n keys + args, not literal text.
        verify(notificationService).createNotification(
                any(MyUser.class),
                eq("notification.event.created.title"),
                eq("notification.event.created.message"),
                eq(List.of("Kickoff")),
                eq(NotificationType.EVENT),
                eq("/events/7"));

        // One WhatsApp per phone carried in the DTO — message details built from scalars, no entity.
        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventMessageDetails> detailsCaptor = ArgumentCaptor.forClass(EventMessageDetails.class);
        verify(whatsAppService, org.mockito.Mockito.times(2))
                .sendEventNotification(phoneCaptor.capture(), detailsCaptor.capture());

        assertThat(phoneCaptor.getAllValues()).containsExactly("+32470000001", "+32470000002");
        assertThat(detailsCaptor.getValue().title()).isEqualTo("Kickoff");
        assertThat(detailsCaptor.getValue().location()).isEqualTo("Ghent");
    }

    @Test
    void handleEventSaved_withNoAttendees_sendsNoWhatsApp() {
        when(myUserRepository.findById(1L)).thenReturn(Optional.of(new MyUser()));

        listener.handleEventSaved(dtoWithAttendees(List.of()));

        verifyNoInteractions(whatsAppService);
        verify(notificationService).createNotification(any(), any(), any(), any(), any(), any());
    }

    /**
     * Structural regression guard: the listener must not hold an EventRepository (or any
     * repository that could reload the event graph) — that was the async-thread collection-load
     * path. If a future change re-injects it, this fails.
     */
    @Test
    void listenerHasNoEventRepositoryDependency() {
        List<String> eventRepoFields = java.util.Arrays.stream(EventNotificationListener.class.getDeclaredFields())
                .filter(f -> EventRepository.class.isAssignableFrom(f.getType()))
                .map(Field::getName)
                .toList();

        assertThat(eventRepoFields)
                .as("listener must not reload the Event entity/collection")
                .isEmpty();
    }
}
