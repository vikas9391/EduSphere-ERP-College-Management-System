package com.collegeerp.Backend.assignment.controller;

import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionResponse;
import com.collegeerp.Backend.assignment.service.AssignmentSubmissionService;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService service;
    private final StudentIdentityService studentIdentityService;

    public AssignmentSubmissionController(
            AssignmentSubmissionService service,
            StudentIdentityService studentIdentityService) {
        this.service = service;
        this.studentIdentityService = studentIdentityService;
    }

    /** Students can submit only under their own resolved domain Student id. */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public AssignmentSubmissionResponse submitAssignment(
            Authentication authentication,
            @RequestBody AssignmentSubmissionRequest request) {
        UserPrincipal principal = principal(authentication);
        request.setStudentId(studentIdentityService.requireStudentId(principal));
        return service.submitAssignment(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping
    public List<AssignmentSubmissionResponse> getAllSubmissions(Authentication authentication) {
        return service.getAllSubmissions(principal(authentication));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping("/assignment/{assignmentId}")
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        return service.getAssignmentSubmissions(assignmentId, principal(authentication));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PutMapping("/{id}/evaluate")
    public AssignmentSubmissionResponse evaluate(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Integer marks,
            @RequestParam(required = false, defaultValue = "") String feedback) {
        return service.evaluateSubmission(id, marks, feedback, principal(authentication));
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
