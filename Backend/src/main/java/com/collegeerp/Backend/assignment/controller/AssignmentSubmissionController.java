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

    public AssignmentSubmissionController(
            AssignmentSubmissionService service) {

        this.service = service;
    }

    /**
     * Only students submit their own work. {@code request.studentId} is deliberately
     * overwritten with the caller's own id from the JWT rather than trusted as-sent -
     * otherwise any authenticated student could submit work under a classmate's id.
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public AssignmentSubmissionResponse submitAssignment(
            Authentication authentication,
            @RequestBody AssignmentSubmissionRequest request) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        request.setStudentId(principal.getId());
        return service.submitAssignment(request);
    }

    /** Lists every student's submissions - restricted so a student can't browse classmates' work/grades. */
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping
    public List<AssignmentSubmissionResponse> getAllSubmissions() {

        return service.getAllSubmissions();
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/assignment/{assignmentId}")
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(
            @PathVariable Long assignmentId) {

        return service.getAssignmentSubmissions(assignmentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/{id}/evaluate")
    public AssignmentSubmissionResponse evaluate(
            @PathVariable Long id,
            @RequestParam Integer marks,
            @RequestParam String feedback) {

        return service.evaluateSubmission(
                id,
                marks,
                feedback);
    }

}