package com.collegeerp.Backend.enrollment.controller;

import com.collegeerp.Backend.enrollment.dto.EnrollmentRequest;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.enrollment.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EnrollmentResponse createEnrollment(
            @RequestBody EnrollmentRequest request) {

        return enrollmentService.createEnrollment(request);
    }

    @GetMapping
    public List<EnrollmentResponse> getAllEnrollments() {

        return enrollmentService.getAllEnrollments();
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentResponse> getMyEnrollments(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return enrollmentService.getMyEnrollments(principal.getEmail(), principal.getId());
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getEnrollment(
            @PathVariable Long id) {

        return enrollmentService.getEnrollment(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public EnrollmentResponse updateEnrollment(
            @PathVariable Long id,
            @RequestBody EnrollmentRequest request) {

        return enrollmentService.updateEnrollment(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteEnrollment(
            @PathVariable Long id) {

        enrollmentService.deleteEnrollment(id);
    }
}
