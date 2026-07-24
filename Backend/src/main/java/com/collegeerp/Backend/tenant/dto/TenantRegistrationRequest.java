package com.collegeerp.Backend.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for registering a new college (tenant). Validation here is the first line of
 * defense before {@code TenantProvisioningService} turns {@code subdomain} into a
 * Postgres schema name, so the pattern below intentionally matches what a schema
 * identifier can safely contain.
 */
@Getter
@Setter
public class TenantRegistrationRequest {

    @NotBlank(message = "College name is required")
    @Size(max = 150, message = "College name must be at most 150 characters")
    private String collegeName;

    @NotBlank(message = "Subdomain is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9-]{3,50}$",
            message = "Subdomain must be 3-50 characters and contain only letters, numbers, and hyphens"
    )
    private String subdomain;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
