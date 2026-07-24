package com.collegeerp.Backend.teacher.portal;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
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

/**
 * Read-only self-service endpoints for the currently logged-in teacher. Every method
 * resolves the teacher id from the JWT-backed {@link UserPrincipal} - never from a path
 * variable or request body - so a teacher can only ever see their own subjects, students,
 * and assignments. Mirrors {@link com.collegeerp.Backend.student.portal.StudentPortalController}.
 * <p>
 * Class-level {@code @PreAuthorize} matters here beyond least-privilege: {@code principal.getId()}
 * is a raw numeric id with no type tag, and teacher/student ids come from independent
 * sequences - without this check, a STUDENT-role JWT could hit these endpoints and have its
 * id happen to match an unrelated teacher's row.
 * <p>
 * Admin-facing CRUD for these same underlying entities lives in their own packages'
 * controllers ({@code subject}, {@code enrollment}, {@code assignment}, {@code attendance})
 * - this controller only adds teacher-scoped, read-only views on top, replacing the
 * frontend's previous approach of fetching every row in the system and filtering client-side
 * (see README_PROGRESS.md, "Teacher/student dashboards still over-fetch").
 */
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
    public ApiResponse<List<EnrollmentResponse>> students(Authentication authentication) {
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
