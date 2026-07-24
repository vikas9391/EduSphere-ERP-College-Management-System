package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceResponse {

    private long totalClasses;
    private long classesAttended;
    private long classesMissed;

    /** 0.0 if the student has no attendance records at all yet. */
    private double overallAttendancePercentage;

    private List<SubjectAttendanceResponse> bySubject;
}
