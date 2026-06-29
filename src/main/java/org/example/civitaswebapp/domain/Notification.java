package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.civitaswebapp.util.StringListConverter;

import java.time.Instant;
import java.util.List;

@Entity
@Data
// id-only identity: never let equals/hashCode/toString walk the user association — see
// [[lombok-data-jpa-entities]].
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MyUser user;

    // optional: link to page
    private String url;

    // i18n: notifications store message-bundle keys (+ runtime args), never literal text, so the
    // title/body can be rendered in the *viewer's* locale at display time via MessageSource.
    private String titleKey;

    private String messageKey;

    @Convert(converter = StringListConverter.class)
    private List<String> messageArgs;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = NotificationStatus.UNREAD;
        if (createdAt == null) createdAt = Instant.now();
    }
}
