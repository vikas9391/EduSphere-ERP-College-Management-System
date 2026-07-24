package com.collegeerp.Backend.subject.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PagedResponse;
import com.collegeerp.Backend.subject.dto.SubjectRequest;
import com.collegeerp.Backend.subject.dto.SubjectResponse;
import com.collegeerp.Backend.subject.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success("Subject created", subjectService.createSubject(request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<SubjectResponse>> getAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(PagedResponse.from(subjectService.getAllSubjects(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> get(@PathVariable Long id) {
        return ApiResponse.success(subjectService.getSubject(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<SubjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success("Subject updated", subjectService.updateSubject(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
    }
}
