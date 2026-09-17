package com.collegeerp.Backend.student.portal;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.dto.*;
import com.collegeerp.Backend.student.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public StudentPortalController(
            StudentDashboardService dashboardService,
            StudentEnrollmentQueryService enrollmentQueryService,
            StudentAttendanceService attendanceService,
            StudentSubjectService subjectService,
            StudentAssignmentService assignmentService,
            StudentResultService resultService,
            StudentTimetableService timetableService,
            StudentNotificationService notificationService,
            StudentIdentityService studentIdentityService,
            ClassEnrollmentRepository classEnrollmentRepository,
            ExamScheduleRepository examScheduleRepository) {
        this.dashboardService = dashboardService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.assignmentService = assignmentService;
        this.resultService = resultService;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
        this.studentIdentityService = studentIdentityService;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.examScheduleRepository = examScheduleRepository;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StudentDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success(dashboardService.getDashboard(studentId(authentication)));
    }

    @GetMapping("/enrollments")
    public ApiResponse<List<ClassEnrollmentResponse>> enrollments(Authentication authentication) {
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

    @GetMapping("/exams")
    public ApiResponse<List<ExamScheduleResponse>> exams(Authentication authentication) {
        Long studentId = studentId(authentication);
        List<Long> classSubjectIds = classEnrollmentRepository.findAllByStudentId(studentId).stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(cs -> cs.getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (classSubjectIds.isEmpty()) return ApiResponse.success(List.of());
        return ApiResponse.success(examScheduleRepository.findUpcomingForStudent(classSubjectIds, LocalDate.now()).stream()
                .map(schedule -> ExamScheduleResponse.builder()
                        .id(schedule.getId())
                        .examId(schedule.getExam().getId())
                        .examName(schedule.getExam().getExamName())
                        .subjectId(schedule.getClassSubject().getSubject() != null ? schedule.getClassSubject().getSubject().getId() : null)
                        .subjectName(schedule.getClassSubject().getSubjectName())
                        .classSubjectId(schedule.getClassSubject().getId())
                        .classId(schedule.getClassSubject().getSchoolClass() != null ? schedule.getClassSubject().getSchoolClass().getId() : null)
                        .className(schedule.getClassSubject().getSchoolClass() != null ? schedule.getClassSubject().getSchoolClass().getName() : null)
                        .examDate(schedule.getExamDate())
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .room(schedule.getRoom())
                        .maxMarks(schedule.getMaxMarks())
                        .build())
                .toList());
    }

    private Long studentId(Authentication authentication) {
        return studentIdentityService.requireStudentId((UserPrincipal) authentication.getPrincipal());
    }
}
