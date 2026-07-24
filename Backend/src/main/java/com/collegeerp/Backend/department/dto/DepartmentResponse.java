package com.collegeerp.Backend.department.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long id;
    private String code;
    private String name;
    private String hod;
    private String description;
}