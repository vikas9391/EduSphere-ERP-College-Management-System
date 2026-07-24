package com.collegeerp.Backend.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;

    @Column(unique = true, nullable = false)
    private String subdomain;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "subscription_plan", nullable = false)
    @Builder.Default
    private String subscriptionPlan = "TRIAL";

    // ACTIVE, EXPIRED, or CANCELLED. Kept as a plain String rather than a Java enum so a
    // new plan/status can be introduced without a migration - the frontend is the only
    // other place that needs to know the valid values.
    @Column(name = "subscription_status", nullable = false)
    @Builder.Default
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}