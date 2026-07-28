package com.collegeerp.Backend.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must be at most 50 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    /** Permission enum names, e.g. "VIEW_TEACHER_PROGRESS". Validated against the live
     *  {@link com.collegeerp.Backend.common.Permission} enum in the service layer. */
    @NotNull(message = "Permissions are required (an empty list is fine for a no-access role)")
    private Set<String> permissions;
}
