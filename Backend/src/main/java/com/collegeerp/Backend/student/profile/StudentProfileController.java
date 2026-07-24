package com.collegeerp.Backend.student.profile;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.auth.StudentPasswordRequest;
import com.collegeerp.Backend.student.auth.StudentProfileResponse;
import com.collegeerp.Backend.student.auth.StudentProfileUpdateRequest;
import com.collegeerp.Backend.student.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Same cross-role id-collision reasoning as {@code StudentPortalController} applies here -
 * see that class's javadoc.
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/profile")
    public ApiResponse<StudentProfileResponse> profile(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(studentProfileService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<StudentProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody StudentProfileUpdateRequest request) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ApiResponse.success("Profile updated", studentProfileService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody StudentPasswordRequest request) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        studentProfileService.changePassword(principal.getId(), request);
        return ApiResponse.success("Password changed successfully", null);
    }
}
