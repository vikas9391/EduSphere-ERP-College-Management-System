package com.collegeerp.Backend.common.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long roleId;
    private String roleName;
    private boolean isActive;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
}
