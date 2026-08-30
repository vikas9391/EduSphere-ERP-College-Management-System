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

/** Read-only self-service endpoints for the currently logged-in student. */
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
    private final StudentIdentityService studentIdentityService;

    public StudentPortalController(
            StudentDashboardService dashboardService,
            StudentEnrollmentQueryService enrollmentQueryService,
            StudentAttendanceService attendanceService,
            StudentSubjectService subjectService,
            StudentAssignmentService assignmentService,
            StudentResultService resultService,
            StudentTimetableService timetableService,
            StudentNotificationService notificationService,
            StudentIdentityService studentIdentityService) {
        this.dashboardService = dashboardService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.assignmentService = assignmentService;
        this.resultService = resultService;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
        this.studentIdentityService = studentIdentityService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StudentDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success(dashboardService.getDashboard(studentId(authentication)));
    }

    @GetMapping("/enrollments")
    public ApiResponse<List<EnrollmentResponse>> enrollments(Authentication authentication) {
        return ApiResponse.success(enrollmentQueryService.getEnrollments(studentId(authentication)));
    }

    @GetMapping("/attendance")
    public ApiResponse<StudentAttendanceResponse> attendance(Authentication authentication) {
        return ApiResponse.success(attendanceService.getAttendance(studentId(authentication)));
    }

    @GetMapping("/subjects")
    public ApiResponse<List<StudentSubjectResponse>> subjects(Authentication authentication) {
        return ApiResponse.success(subjectService.getSubjects(studentId(authentication)));
    }

    @GetMapping("/assignments")
    public ApiResponse<List<StudentAssignmentResponse>> assignments(Authentication authentication) {
        return ApiResponse.success(assignmentService.getAssignments(studentId(authentication)));
    }

    @GetMapping("/results")
    public ApiResponse<OverallResultResponse> results(Authentication authentication) {
        return ApiResponse.success(resultService.getResults(studentId(authentication)));
    }

    @GetMapping("/timetable")
    public ApiResponse<StudentTimetableResponse> timetable(Authentication authentication) {
        return ApiResponse.success(timetableService.getTimetable(studentId(authentication)));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<NotificationResponse>> notifications(Authentication authentication) {
        return ApiResponse.success(notificationService.getNotifications(studentId(authentication)));
    }

    private Long studentId(Authentication authentication) {
        return studentIdentityService.requireStudentId((UserPrincipal) authentication.getPrincipal());
    }
}
