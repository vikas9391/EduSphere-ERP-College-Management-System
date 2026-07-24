package com.collegeerp.Backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Payload for {@code PATCH /api/tenants/{id}/subscription}. Purely bookkeeping - unlike
 * {@code TenantStatusUpdateRequest}, changing the subscription status here does NOT by
 * itself block logins. If you want an expired subscription to actually lock the college
 * out, suspend it via the status endpoint too.
 */
public record TenantSubscriptionUpdateRequest(

        @NotBlank(message = "Plan is required")
        String plan,

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|EXPIRED|CANCELLED", message = "Status must be ACTIVE, EXPIRED, or CANCELLED")
        String status,

        LocalDateTime expiresAt
) {}
