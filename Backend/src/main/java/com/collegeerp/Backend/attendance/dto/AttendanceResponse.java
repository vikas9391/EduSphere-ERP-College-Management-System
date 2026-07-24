package com.collegeerp.Backend.attendance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;

    private Long enrollmentId;

    private Long studentId;

    private String studentName;

    private Long subjectId;

    private String subjectName;

    private LocalDate attendanceDate;

    private String status;

    private String remarks;

}