package com.collegeerp.Backend.teacher.portal;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.subject.dto.SubjectResponse;
import com.collegeerp.Backend.teacher.dto.TeacherAssignmentResponse;
import com.collegeerp.Backend.teacher.dto.TeacherDashboardResponse;
import com.collegeerp.Backend.teacher.service.TeacherAssignmentQueryService;
import com.collegeerp.Backend.teacher.service.TeacherDashboardService;
import com.collegeerp.Backend.teacher.service.TeacherStudentQueryService;
import com.collegeerp.Backend.teacher.service.TeacherSubjectQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only self-service endpoints for the currently logged-in teacher. */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherPortalController {

    private final TeacherDashboardService dashboardService;
    private final TeacherSubjectQueryService subjectQueryService;
    private final TeacherStudentQueryService studentQueryService;
    private final TeacherAssignmentQueryService assignmentQueryService;

    public TeacherPortalController(
            TeacherDashboardService dashboardService,
            TeacherSubjectQueryService subjectQueryService,
            TeacherStudentQueryService studentQueryService,
            TeacherAssignmentQueryService assignmentQueryService) {
        this.dashboardService = dashboardService;
        this.subjectQueryService = subjectQueryService;
        this.studentQueryService = studentQueryService;
        this.assignmentQueryService = assignmentQueryService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<TeacherDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success(dashboardService.getDashboard(teacherId(authentication)));
    }

    @GetMapping("/subjects")
    public ApiResponse<List<SubjectResponse>> subjects(Authentication authentication) {
        return ApiResponse.success(subjectQueryService.getSubjects(teacherId(authentication)));
    }

    @GetMapping("/students")
    public ApiResponse<List<ClassEnrollmentResponse>> students(Authentication authentication) {
        return ApiResponse.success(studentQueryService.getStudents(teacherId(authentication)));
    }

    @GetMapping("/assignments")
    public ApiResponse<List<TeacherAssignmentResponse>> assignments(Authentication authentication) {
        return ApiResponse.success(assignmentQueryService.getAssignments(teacherId(authentication)));
    }

    private Long teacherId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
