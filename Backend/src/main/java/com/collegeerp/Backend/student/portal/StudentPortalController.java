package com.collegeerp.Backend.student.portal;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.dto.*;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.student.service.*;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
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
    private final StudentRepository studentRepository;

    public StudentPortalController(
            StudentDashboardService dashboardService,
            StudentEnrollmentQueryService enrollmentQueryService,
            StudentAttendanceService attendanceService,
            StudentSubjectService subjectService,
            StudentAssignmentService assignmentService,
            StudentResultService resultService,
            StudentTimetableService timetableService,
            StudentNotificationService notificationService,
            StudentRepository studentRepository) {
        this.dashboardService = dashboardService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.assignmentService = assignmentService;
        this.resultService = resultService;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
        this.studentRepository = studentRepository;
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

    /**
     * UserPrincipal.id is the authenticated User id, not necessarily the Student id.
     * Resolve the student by the email embedded in the authenticated JWT so every
     * student-scoped endpoint uses the same canonical Student id as announcements.
     */
    private Long studentId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Student student = studentRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for authenticated user"));
        return student.getId();
    }
}
