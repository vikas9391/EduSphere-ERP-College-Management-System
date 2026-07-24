package com.collegeerp.Backend.tenant.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/tenants/{id}/status}. Setting {@code isActive} to false
 * suspends the college: {@code AuthController#login} refuses every login for that tenant
 * (staff, teacher, and student alike) with a 403 until it's reactivated. No data is
 * touched - this is reversible, unlike {@code DELETE /api/tenants/{id}}.
 */
public record TenantStatusUpdateRequest(

        @NotNull(message = "isActive is required")
        Boolean isActive
) {}
