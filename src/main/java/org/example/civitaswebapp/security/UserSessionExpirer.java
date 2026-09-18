package org.example.civitaswebapp.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Forces a user to log in again. The role lives in the {@link MyUserPrincipal} stored in the HTTP
 * session at login, so a role change would otherwise not apply (a demoted ADMIN would keep write
 * access) until the session ends. Expired sessions are redirected to the login page by the
 * concurrent-session filter configured in {@code SecurityConfig}.
 */
@Component
public class UserSessionExpirer {

    private final SessionRegistry sessionRegistry;

    public UserSessionExpirer(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void expireSessionsOf(Long userId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof MyUserPrincipal p && Objects.equals(p.getId(), userId)) {
                sessionRegistry.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
            }
        }
    }
}
