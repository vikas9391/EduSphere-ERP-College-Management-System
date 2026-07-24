package com.collegeerp.Backend.teacher.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TeacherResponse {

    private Long id;

    private String employeeId;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String gender;

    private String qualification;

    private String specialization;

    private Integer experience;

    private LocalDate joiningDate;
}