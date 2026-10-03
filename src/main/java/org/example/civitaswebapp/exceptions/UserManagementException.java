package org.example.civitaswebapp.exceptions;

/**
 * Business-rule violation in the user/role management screen. Carries an i18n message key (rather
 * than display text) so the controller can render it in the viewer's locale.
 */
public class UserManagementException extends RuntimeException {

    private final String messageKey;

    public UserManagementException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
