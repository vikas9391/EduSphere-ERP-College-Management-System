package com.collegeerp.Backend.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequest {

    @NotBlank(message = "Department code is required")
    @Size(max = 20, message = "Department code must be at most 20 characters")
    private String code;

    @NotBlank(message = "Department name is required")
    @Size(max = 150, message = "Department name must be at most 150 characters")
    private String name;

    @Size(max = 150, message = "HOD name must be at most 150 characters")
    private String hod;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;
}
