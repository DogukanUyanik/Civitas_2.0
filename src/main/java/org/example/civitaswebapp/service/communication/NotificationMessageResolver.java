package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.Notification;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Resolves a stored {@link Notification} (which holds only i18n keys + arguments) into
 * display-ready title/body text for a given locale. Shared by the server-rendered notifications
 * page and the JSON dropdown endpoint so both render in the viewer's currently selected language.
 *
 * <p>If a key is missing from the bundle, the key itself is returned as the default — visible and
 * diagnosable, never an exception.
 */
@Component
public class NotificationMessageResolver {

    private final MessageSource messageSource;

    public NotificationMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolveTitle(Notification notification, Locale locale) {
        return resolve(notification.getTitleKey(), null, locale);
    }

    public String resolveMessage(Notification notification, Locale locale) {
        Object[] args = toArgs(notification.getMessageArgs());
        return resolve(notification.getMessageKey(), args, locale);
    }

    private String resolve(String key, Object[] args, Locale locale) {
        if (key == null) {
            return "";
        }
        // Use the key itself as the default message so a missing translation degrades gracefully.
        return messageSource.getMessage(key, args, key, locale);
    }

    private Object[] toArgs(List<String> args) {
        return (args == null || args.isEmpty()) ? null : args.toArray();
    }
}
