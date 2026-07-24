package com.collegeerp.Backend.enrollment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRequest {

    private Long studentId;

    private Long subjectId;

    private String academicYear;

    private Integer semester;

    private LocalDate enrollmentDate;
}