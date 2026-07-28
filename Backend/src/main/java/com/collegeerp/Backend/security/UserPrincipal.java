package com.collegeerp.Backend.security;

import java.util.Objects;
import java.util.Set;

/**
 * The authenticated principal placed in the Spring Security context by {@link JwtAuthFilter}.
 * Controllers can safely cast {@code Authentication#getPrincipal()} to this type.
 * <p>
 * {@link #toString()} returns the email because {@code UsernamePasswordAuthenticationToken#getName()}
 * falls back to {@code principal.toString()} for non-{@code UserDetails} principals - without this
 * override, {@code Authentication#getName()} would return the default {@code Object} identity string
 * instead of the user's email.
 */
public class UserPrincipal {

    private final Long id;
    private final String email;
    private final String role;
    private final Set<String> permissions;

    public UserPrincipal(Long id, String email, String role) {
        this(id, email, role, Set.of());
    }

    public UserPrincipal(Long id, String email, String role, Set<String> permissions) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.permissions = permissions == null ? Set.of() : permissions;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    /**
     * Fine-grained permissions (see {@link com.collegeerp.Backend.common.Permission})
     * embedded in the JWT at login time from the user's assigned Role. Empty for
     * teachers/students/the super admin, who authenticate through separate paths that
     * don't go through the Role/Permission system.
     */
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
