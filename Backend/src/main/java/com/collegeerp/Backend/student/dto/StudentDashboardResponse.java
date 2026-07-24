package com.collegeerp.Backend.student.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardResponse {

    private Long studentId;
    private String studentName;
    private String rollNumber;

    /**
     * Derived from the student's most recent enrollment's subject -> course -> department,
     * since the schema has no direct Student -> Department FK (a student's department/course/
     * semester are only ever recorded through their subject enrollments). Null if the student
     * has no enrollments yet.
     */
    private String department;
    private String course;
    private Integer semester;

    /** 0.0 if the student has no PUBLISHED results yet. */
    private Double cgpa;

    /** 0.0 if the student has no attendance records yet. */
    private Double attendancePercentage;

    private Integer totalSubjects;
    private Integer pendingAssignments;
    private Integer upcomingExams;
    private Integer notificationsCount;
}
