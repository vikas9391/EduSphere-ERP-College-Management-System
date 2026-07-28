package com.collegeerp.Backend.common.dto;

import lombok.*;

import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private boolean isSystemRole;
    private Set<String> permissions;
}
