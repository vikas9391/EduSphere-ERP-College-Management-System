package com.collegeerp.Backend.common;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    /**
     * True for the built-in roles seeded per tenant (currently just ADMIN, seeded in
     * {@code TenantProvisioningService}). System roles can't be edited or deleted
     * through {@code RoleController} - they're the floor every custom role's
     * permissions are checked against via the privilege-escalation guard.
     */
    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private boolean isSystemRole = false;

    /**
     * Permission enum names (see {@link Permission}), stored as plain strings rather
     * than an enum-typed collection so a role row doesn't become unreadable if a
     * permission is ever renamed/removed in code - service-layer validation is what
     * keeps this set honest against the live {@link Permission} enum.
     * <p>
     * EAGER because {@code User.role} is EAGER and permissions need to be available
     * to embed into the JWT right after login, with {@code spring.jpa.open-in-view}
     * disabled (no lazy loading once the request-scoped session closes).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false)
    @Builder.Default
    private Set<String> permissions = new HashSet<>();
}
