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
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public AssignmentResponse createAssignment(
            Authentication authentication,
            @RequestBody AssignmentRequest request) {
        return assignmentService.createAssignment(request, principal(authentication));
    }

    @GetMapping
    public List<AssignmentResponse> getAllAssignments(Authentication authentication) {
        return assignmentService.getAllAssignments(principal(authentication));
    }

    @GetMapping("/{id}")
    public AssignmentResponse getAssignment(Authentication authentication, @PathVariable Long id) {
        return assignmentService.getAssignment(id, principal(authentication));
    }

    @PutMapping("/{id}")
    public AssignmentResponse updateAssignment(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody AssignmentRequest request) {
        return assignmentService.updateAssignment(id, request, principal(authentication));
    }

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
