package com.collegeerp.Backend.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single-use, expiring token issued by the "forgot password" flow for a
 * tenant-scoped account. Lives in each tenant's schema (no explicit {@code schema}
 * on {@code @Table}, same as {@link User} (Teacher is a role on this same table now,
 * not a separate entity)/{@link com.collegeerp.Backend.student.entity.Student} -
 * resolved via {@link com.collegeerp.Backend.tenant.TenantContext} at request time).
 * <p>
 * {@code accountType} says which repository owns {@code email} when the token is
 * redeemed - {@code "STAFF"} (users table - staff, admin, and teacher accounts all
 * live here now) or {@code "STUDENT"} - see {@code PasswordResetService}'s
 * ACCOUNT_TYPE_* constants, which use the same values.
 * <p>
 * The public-schema equivalent for the super admin account is
 * {@link SuperAdminPasswordResetToken}.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
