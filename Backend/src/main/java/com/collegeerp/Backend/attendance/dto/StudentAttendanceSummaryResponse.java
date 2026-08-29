package com.collegeerp.Backend.attendance.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceSummaryResponse {
    private int totalClasses;
    private int classesAttended;
    private int classesMissed;
    private double overallAttendancePercentage;
    private List<SubjectAttendanceSummary> bySubject;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectAttendanceSummary {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private int totalClasses;
        private int classesAttended;
        private int classesMissed;
        private double attendancePercentage;
    }
}
