package com.collegeerp.Backend.student.portal;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.dto.*;
import com.collegeerp.Backend.student.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only self-service endpoints for the currently logged-in student. Every method resolves
 * the student id from the JWT-backed {@link UserPrincipal} - never from a path variable or
 * request body - so a student can only ever see their own data. This mirrors the pattern
 * already used by {@link com.collegeerp.Backend.student.profile.StudentProfileController}.
 * <p>
 * Admin-facing CRUD for these same underlying entities lives in their own packages'
 * controllers ({@code enrollment}, {@code attendance}, {@code assignment}, {@code result},
 * {@code subject}) - this controller only adds student-scoped, read-only views on top.
 * <p>
 * Class-level {@code @PreAuthorize} matters here beyond least-privilege: {@code principal.getId()}
 * is a raw numeric id with no type tag, and teacher/student ids come from independent sequences -
 * without this check, a TEACHER-role JWT could hit these endpoints and have its id happen to
 * match an unrelated student's row.
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPortalController {

    private final StudentDashboardService dashboardService;
    private final StudentEnrollmentQueryService enrollmentQueryService;
    private final StudentAttendanceService attendanceService;
    private final StudentSubjectService subjectService;
    private final StudentAssignmentService assignmentService;
    private final StudentResultService resultService;
    private final StudentTimetableService timetableService;
    private final StudentNotificationService notificationService;

    public StudentPortalController(
            StudentDashboardService dashboardService,
            StudentEnrollmentQueryService enrollmentQueryService,
            StudentAttendanceService attendanceService,
            StudentSubjectService subjectService,
            StudentAssignmentService assignmentService,
            StudentResultService resultService,
            StudentTimetableService timetableService,
            StudentNotificationService notificationService) {

        this.dashboardService = dashboardService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.assignmentService = assignmentService;
        this.resultService = resultService;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StudentDashboardResponse> dashboard(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(dashboardService.getDashboard(studentId));
    }

    @GetMapping("/enrollments")
    public ApiResponse<List<EnrollmentResponse>> enrollments(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(enrollmentQueryService.getEnrollments(studentId));
    }

    @GetMapping("/attendance")
    public ApiResponse<StudentAttendanceResponse> attendance(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(attendanceService.getAttendance(studentId));
    }

    @GetMapping("/subjects")
    public ApiResponse<List<StudentSubjectResponse>> subjects(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(subjectService.getSubjects(studentId));
    }

    @GetMapping("/assignments")
    public ApiResponse<List<StudentAssignmentResponse>> assignments(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(assignmentService.getAssignments(studentId));
    }

    @GetMapping("/results")
    public ApiResponse<OverallResultResponse> results(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(resultService.getResults(studentId));
    }

    /** See {@link StudentTimetableService} - this endpoint currently returns placeholder data. */
    @GetMapping("/timetable")
    public ApiResponse<StudentTimetableResponse> timetable(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(timetableService.getTimetable(studentId));
    }

    /** See {@link StudentNotificationService} - this endpoint currently returns placeholder data. */
    @GetMapping("/notifications")
    public ApiResponse<List<NotificationResponse>> notifications(Authentication authentication) {
        Long studentId = studentId(authentication);
        return ApiResponse.success(notificationService.getNotifications(studentId));
    }

    private Long studentId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
