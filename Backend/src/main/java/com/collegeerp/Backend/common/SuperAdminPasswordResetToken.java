package com.collegeerp.Backend.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Public-schema counterpart to {@link PasswordResetToken} - a super admin isn't
 * scoped to any tenant, so its reset tokens are pinned to the public schema the
 * same way {@link SuperAdmin} itself is (see that class's Javadoc).
 */
@Entity
@Table(name = "super_admin_password_reset_tokens", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuperAdminPasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
