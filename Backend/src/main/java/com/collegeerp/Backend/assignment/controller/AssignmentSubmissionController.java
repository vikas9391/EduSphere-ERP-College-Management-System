package com.collegeerp.Backend.assignment.controller;

import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionResponse;
import com.collegeerp.Backend.assignment.service.AssignmentSubmissionService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService service;

    public AssignmentSubmissionController(AssignmentSubmissionService service) {
        this.service = service;
    }

    /** Students can submit only under their own authenticated student id. */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public AssignmentSubmissionResponse submitAssignment(
            Authentication authentication,
            @RequestBody AssignmentSubmissionRequest request) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        request.setStudentId(principal.getId());
        return service.submitAssignment(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping
    public List<AssignmentSubmissionResponse> getAllSubmissions() {
        return service.getAllSubmissions();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping("/assignment/{assignmentId}")
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(
            @PathVariable Long assignmentId) {
        return service.getAssignmentSubmissions(assignmentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PutMapping("/{id}/evaluate")
    public AssignmentSubmissionResponse evaluate(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Integer marks,
            @RequestParam(required = false, defaultValue = "") String feedback) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return service.evaluateSubmission(id, marks, feedback, principal);
    }
}
