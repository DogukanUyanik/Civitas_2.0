package org.example.civitaswebapp.security;

import org.example.civitaswebapp.domain.MyUserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Lightweight, immutable Spring Security principal.
 *
 * <p>Intentionally NOT a JPA entity. It carries only scalar identity data and holds no
 * Hibernate-managed collections. The principal is stored in the HTTP session and shared
 * across every request thread (and is reachable from async notification threads), so backing
 * it with a managed {@code MyUser} entity caused two threads to touch the same Hibernate
 * {@code PersistentCollection} concurrently — surfacing as {@code ConcurrentModificationException}
 * in Hibernate's {@code injectLoadedState} during collection loading.
 *
 * <p>Any code that needs the managed {@code MyUser} or {@code Union} must reload it fresh from
 * the repository within its own transaction (see {@code MyUserService#getLoggedInUser()}),
 * never reach into this principal for a managed entity graph.
 */
public final class MyUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final MyUserRole role;
    private final UUID unionId;

    public MyUserPrincipal(Long id, String username, String password, MyUserRole role, UUID unionId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.unionId = unionId;
    }

    public Long getId() {
        return id;
    }

    public UUID getUnionId() {
        return unionId;
    }

    public MyUserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
