package com.collegeerp.Backend.enrollment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;

    private Long studentId;

    private String studentName;

    private String admissionNo;

    private Long subjectId;

    private String subjectName;

    private String subjectCode;

    private String courseName;

    private String teacherName;

    private String academicYear;

    private Integer semester;

    private LocalDate enrollmentDate;

    private String status;
}