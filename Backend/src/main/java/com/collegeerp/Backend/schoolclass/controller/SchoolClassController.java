package com.collegeerp.Backend.schoolclass.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.schoolclass.dto.AddStudentsRequest;
import com.collegeerp.Backend.schoolclass.dto.ClassStudentResponse;
import com.collegeerp.Backend.schoolclass.dto.SchoolClassRequest;
import com.collegeerp.Backend.schoolclass.dto.SchoolClassResponse;
import com.collegeerp.Backend.schoolclass.service.SchoolClassService;
import com.collegeerp.Backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Teacher-owned classes (batches/sections) and their student rosters. Every endpoint
 * here is scoped to "teachers manage their own classes" - see
 * {@link SchoolClassService#requireOwnerOrAdmin} - rather than relying on the
 * application-wide authorization gap tracked separately (every endpoint elsewhere
 * currently just requires *some* valid token). Subject management lives in
 * {@link ClassSubjectController}.
 */
@RestController
@RequestMapping("/api/classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SchoolClassResponse> create(Authentication authentication,
                                                     @Valid @RequestBody SchoolClassRequest request) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success("Class created",
                schoolClassService.createClass(principal.getId(), principal.getRole(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<SchoolClassResponse>> getMine(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(schoolClassService.getMyClasses(principal.getId(), principal.getRole()));
    }

    /** Student-facing counterpart to {@code /mine}: classes the calling student belongs to. */
    @GetMapping("/mine-as-student")
    public ApiResponse<List<SchoolClassResponse>> getMineAsStudent(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(schoolClassService.getMyClassesAsStudent(principal.getId(), principal.getRole()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SchoolClassResponse> get(Authentication authentication, @PathVariable Long id) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(schoolClassService.getClass(id, principal.getId(), principal.getRole()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        UserPrincipal principal = principal(authentication);
        schoolClassService.deleteClass(id, principal.getId(), principal.getRole());
    }

    @GetMapping("/{id}/students")
    public ApiResponse<List<ClassStudentResponse>> getRoster(Authentication authentication, @PathVariable Long id) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(schoolClassService.getRoster(id, principal.getId(), principal.getRole()));
    }

    @PostMapping("/{id}/students")
    public ApiResponse<List<ClassStudentResponse>> addStudents(Authentication authentication,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody AddStudentsRequest request) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success("Students added",
                schoolClassService.addStudents(id, principal.getId(), principal.getRole(), request));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(Authentication authentication, @PathVariable Long id, @PathVariable Long studentId) {
        UserPrincipal principal = principal(authentication);
        schoolClassService.removeStudent(id, studentId, principal.getId(), principal.getRole());
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
