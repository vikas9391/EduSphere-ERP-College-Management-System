package com.collegeerp.Backend.schoolclass.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectRequest;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectResponse;
import com.collegeerp.Backend.schoolclass.service.ClassSubjectService;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassSubjectController {

    private final ClassSubjectService classSubjectService;
    private final StudentIdentityService studentIdentityService;

    public ClassSubjectController(ClassSubjectService classSubjectService,
                                  StudentIdentityService studentIdentityService) {
        this.classSubjectService = classSubjectService;
        this.studentIdentityService = studentIdentityService;
    }

    @PostMapping("/{classId}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassSubjectResponse> createSubject(Authentication authentication,
                                                             @PathVariable Long classId,
                                                             @Valid @RequestBody ClassSubjectRequest request) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success("Subject created",
                classSubjectService.createSubject(classId, principal.getId(), principal.getRole(), request));
    }

    @PostMapping("/{classId}/subjects/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<ClassSubjectResponse>> createSubjects(Authentication authentication,
                                                                    @PathVariable Long classId,
                                                                    @Valid @RequestBody List<ClassSubjectRequest> requests) {
        UserPrincipal principal = principal(authentication);
        List<ClassSubjectResponse> created = requests.stream()
                .map(r -> classSubjectService.createSubject(classId, principal.getId(), principal.getRole(), r))
                .toList();
        return ApiResponse.success("Subjects created", created);
    }

    @GetMapping("/{classId}/subjects")
    public ApiResponse<List<ClassSubjectResponse>> getSubjects(Authentication authentication, @PathVariable Long classId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getSubjects(classId, principal.getId(), principal.getRole()));
    }

    @GetMapping("/{classId}/subjects/mine")
    public ApiResponse<List<ClassSubjectResponse>> getSubjectsForStudent(Authentication authentication, @PathVariable Long classId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getSubjectsForStudent(
                classId, studentIdentityService.requireStudentId(principal), principal.getRole()));
    }

    @DeleteMapping("/{classId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(Authentication authentication, @PathVariable Long classId, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        classSubjectService.deleteSubject(subjectId, principal.getId(), principal.getRole());
    }

    @GetMapping("/enrollments/mine")
    public ApiResponse<List<ClassEnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getMyEnrollments(
                studentIdentityService.requireStudentId(principal), principal.getRole()));
    }

    @GetMapping("/subjects/{subjectId}/enrollments")
    public ApiResponse<List<ClassEnrollmentResponse>> getEnrollments(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getEnrollments(subjectId, principal.getId(), principal.getRole()));
    }

    @PostMapping("/subjects/{subjectId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassEnrollmentResponse> selfEnroll(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success("Enrolled", classSubjectService.selfEnroll(
                subjectId, studentIdentityService.requireStudentId(principal), principal.getRole()));
    }

    @DeleteMapping("/subjects/{subjectId}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selfDrop(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        classSubjectService.selfDrop(subjectId, studentIdentityService.requireStudentId(principal), principal.getRole());
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
