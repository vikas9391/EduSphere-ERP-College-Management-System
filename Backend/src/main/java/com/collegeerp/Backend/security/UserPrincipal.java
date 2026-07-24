package com.collegeerp.Backend.security;

import java.util.Objects;

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

    public UserPrincipal(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
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
