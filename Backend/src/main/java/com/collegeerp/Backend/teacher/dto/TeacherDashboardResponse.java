package com.collegeerp.Backend.teacher.dto;

import lombok.*;

import java.util.List;

/**
 * Aggregates everything the teacher dashboard page renders into a single response, so the
 * frontend makes one API call instead of fetching every assignment/attendance/subject/
 * enrollment row in the system and filtering client-side (see README_PROGRESS.md,
 * "Teacher/student dashboards still over-fetch"). Mirrors
 * {@code student.dto.StudentDashboardResponse}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDashboardResponse {

    private Long teacherId;
    private String teacherName;

    private Integer totalSubjects;

    /** Distinct students across every subject this teacher teaches. */
    private Integer totalStudents;

    /** Submitted but not yet evaluated, across every assignment this teacher owns. */
    private Integer pendingReviewCount;

    /** Subjects this teacher teaches with no attendance record marked for today yet. */
    private Integer attendancePendingToday;

    private Integer upcomingClassesCount;

    private List<SubjectAssignmentCountResponse> assignmentsPerSubject;

    /** Last 7 days, oldest first. */
    private List<AttendanceTrendPointResponse> attendanceTrend;

    /** Up to 5 most recently-due assignments this teacher owns. */
    private List<TeacherAssignmentResponse> recentAssignments;

    /**
     * <b>Placeholder data</b> - see {@link com.collegeerp.Backend.teacher.service.TeacherScheduleService}.
     * Subjects are real; day/time/room assignments are not read from any real schedule.
     */
    private List<TeacherScheduleEntryResponse> todaysSchedule;
    private boolean schedulePlaceholder;

    /**
     * <b>Placeholder data</b> - see {@link com.collegeerp.Backend.teacher.service.TeacherAnnouncementService}.
     * Nothing here is teacher-specific or persisted.
     */
    private List<TeacherAnnouncementResponse> announcements;
    private boolean announcementsPlaceholder;
}
