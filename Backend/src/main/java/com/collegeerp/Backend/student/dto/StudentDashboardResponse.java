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
     * Resolved from the student's directly-assigned course (Student -> Course -> Department)
     * when one has been set; falls back to the most recent enrollment's subject -> course ->
     * department for students with no course assigned yet. Null if neither is available.
     * Semester still comes only from enrollment, since there's no direct Student -> Semester
     * field.
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
