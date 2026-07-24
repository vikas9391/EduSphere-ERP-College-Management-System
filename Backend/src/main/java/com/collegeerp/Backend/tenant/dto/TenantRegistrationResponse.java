package com.collegeerp.Backend.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TenantRegistrationResponse {

    private UUID tenantId;

    private String collegeName;

    private String schemaName;

    private String message;
}