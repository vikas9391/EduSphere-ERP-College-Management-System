package com.collegeerp.Backend.assignment.controller;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.service.AssignmentService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PostMapping
    public AssignmentResponse createAssignment(
            Authentication authentication,
            @RequestBody AssignmentRequest request) {
        return assignmentService.createAssignment(request, principal(authentication));
    }

    @GetMapping
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentService.getAllAssignments();
    }

    @GetMapping("/{id}")
    public AssignmentResponse getAssignment(@PathVariable Long id) {
        return assignmentService.getAssignment(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public AssignmentResponse updateAssignment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody AssignmentRequest request) {
        return assignmentService.updateAssignment(id, request, principal(authentication));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteAssignment(
            Authentication authentication,
            @PathVariable Long id) {
        assignmentService.deleteAssignment(id, principal(authentication));
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
