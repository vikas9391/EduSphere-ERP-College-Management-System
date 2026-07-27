package com.collegeerp.Backend.tenant.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collegeerp.Backend.tenant.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySubdomain(String subdomain);

    boolean existsBySchemaName(String schemaName);

    Optional<Tenant> findBySubdomain(String subdomain);

    /** Still-active tenants whose subscription expiry has passed - candidates for auto-suspension. */
    List<Tenant> findByIsActiveTrueAndSubscriptionExpiresAtBefore(LocalDateTime cutoff);
}