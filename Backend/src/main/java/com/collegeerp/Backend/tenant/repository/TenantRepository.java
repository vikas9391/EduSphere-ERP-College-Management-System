package com.collegeerp.Backend.tenant.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collegeerp.Backend.tenant.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySubdomain(String subdomain);

    boolean existsBySchemaName(String schemaName);

    Optional<Tenant> findBySubdomain(String subdomain);
}