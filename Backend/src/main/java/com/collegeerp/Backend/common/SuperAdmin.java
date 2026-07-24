package com.collegeerp.Backend.common;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Platform-level operator account: the only role allowed to register new colleges
 * (tenants). Lives in the "public" schema — explicitly pinned there via
 * {@code @Table(schema = "public")}, the same pattern {@code Tenant} uses, so it's
 * always reachable regardless of whatever tenant schema {@link com.collegeerp.Backend.tenant.TenantContext}
 * currently holds for the request.
 */
@Entity
@Table(name = "super_admins", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuperAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
