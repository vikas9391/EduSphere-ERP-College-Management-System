package com.collegeerp.Backend.student.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAttendanceResponse {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;

    private long totalClasses;
    private long classesAttended;
    private long classesMissed;
    private double attendancePercentage;
}
