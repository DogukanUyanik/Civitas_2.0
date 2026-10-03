package org.example.civitaswebapp.domain;

/**
 * Per-user access level. ADMIN has full write access; VIEWER is read-only. Every write endpoint is
 * guarded with {@code @PreAuthorize("hasRole('ADMIN')")}, so any role other than ADMIN is read-only
 * by default. Persisted as a string, so declaration order is irrelevant.
 */
public enum MyUserRole {
    ADMIN, VIEWER
}
