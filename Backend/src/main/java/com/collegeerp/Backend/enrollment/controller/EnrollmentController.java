package com.collegeerp.Backend.enrollment.controller;

import com.collegeerp.Backend.enrollment.dto.EnrollmentRequest;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.enrollment.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentIdentityService studentIdentityService;

    public EnrollmentController(EnrollmentService enrollmentService, StudentIdentityService studentIdentityService) {
        this.enrollmentService = enrollmentService;
        this.studentIdentityService = studentIdentityService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EnrollmentResponse createEnrollment(
            @RequestBody EnrollmentRequest request) {

        return enrollmentService.createEnrollment(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EnrollmentResponse> getAllEnrollments() {

        return enrollmentService.getAllEnrollments();
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentResponse> getMyEnrollments(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return enrollmentService.getMyEnrollments(principal.getEmail(), studentIdentityService.requireStudentId(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
