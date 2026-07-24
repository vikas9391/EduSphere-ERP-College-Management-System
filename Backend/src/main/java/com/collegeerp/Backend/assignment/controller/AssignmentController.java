package com.collegeerp.Backend.assignment.controller;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.service.AssignmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(
            AssignmentService assignmentService) {

        this.assignmentService = assignmentService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping
    public AssignmentResponse createAssignment(
            @RequestBody AssignmentRequest request) {

        return assignmentService.createAssignment(request);
    }

    @GetMapping
    public List<AssignmentResponse> getAllAssignments() {

        return assignmentService.getAllAssignments();
    }

    @GetMapping("/{id}")
    public AssignmentResponse getAssignment(
            @PathVariable Long id) {

        return assignmentService.getAssignment(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public AssignmentResponse updateAssignment(
            @PathVariable Long id,
            @RequestBody AssignmentRequest request) {

        return assignmentService.updateAssignment(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteAssignment(
            @PathVariable Long id) {

        assignmentService.deleteAssignment(id);
    }
}