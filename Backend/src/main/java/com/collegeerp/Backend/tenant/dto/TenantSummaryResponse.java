package com.collegeerp.Backend.tenant.dto;

import com.collegeerp.Backend.tenant.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TenantSummaryResponse {

    private UUID tenantId;
    private String collegeName;
    private String subdomain;
    private String schemaName;
    private boolean isActive;
    private String subscriptionPlan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionExpiresAt;
    private LocalDateTime createdAt;

    public static TenantSummaryResponse from(Tenant tenant) {
        return new TenantSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getSchemaName(),
                tenant.isActive(),
                tenant.getSubscriptionPlan(),
                tenant.getSubscriptionStatus(),
                tenant.getSubscriptionExpiresAt(),
                tenant.getCreatedAt()
        );
    }
}
