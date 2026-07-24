package com.collegeerp.Backend.schoolclass.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectRequest;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectResponse;
import com.collegeerp.Backend.schoolclass.service.ClassSubjectService;
import com.collegeerp.Backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassSubjectController {

    private final ClassSubjectService classSubjectService;

    public ClassSubjectController(ClassSubjectService classSubjectService) {
        this.classSubjectService = classSubjectService;
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

    /**
     * Sets up every subject for the term in one call - e.g. "create all this semester's
     * subjects" for a newly-created class. Each subject is created independently (same
     * validation and auto-enrollment as the single-create endpoint); if one item in the
     * list fails (duplicate code, over the class's max-subjects cap, etc.) the request
     * stops there and returns an error - subjects already created earlier in the list
     * remain created. Re-submitting the remaining items is safe since duplicate codes
     * are rejected.
     */
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

    /** Student-facing counterpart to the list above: this class's subjects, tagged with the caller's own enrollment status. */
    @GetMapping("/{classId}/subjects/mine")
    public ApiResponse<List<ClassSubjectResponse>> getSubjectsForStudent(Authentication authentication, @PathVariable Long classId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getSubjectsForStudent(classId, principal.getId(), principal.getRole()));
    }

    @DeleteMapping("/{classId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(Authentication authentication, @PathVariable Long classId, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        classSubjectService.deleteSubject(subjectId, principal.getId(), principal.getRole());
    }

    @GetMapping("/subjects/{subjectId}/enrollments")
    public ApiResponse<List<ClassEnrollmentResponse>> getEnrollments(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success(classSubjectService.getEnrollments(subjectId, principal.getId(), principal.getRole()));
    }

    /** A student opting into an ELECTIVE subject of a class they're a roster member of. */
    @PostMapping("/subjects/{subjectId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassEnrollmentResponse> selfEnroll(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        return ApiResponse.success("Enrolled",
                classSubjectService.selfEnroll(subjectId, principal.getId(), principal.getRole()));
    }

    /** A student dropping an ELECTIVE subject they previously self-enrolled in. */
    @DeleteMapping("/subjects/{subjectId}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selfDrop(Authentication authentication, @PathVariable Long subjectId) {
        UserPrincipal principal = principal(authentication);
        classSubjectService.selfDrop(subjectId, principal.getId(), principal.getRole());
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
