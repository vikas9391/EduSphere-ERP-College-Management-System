package com.collegeerp.Backend.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleAssignmentRequest {

    @NotNull(message = "A role must be assigned")
    private Long roleId;
}
