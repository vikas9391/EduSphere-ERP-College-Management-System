package com.collegeerp.Backend.teacher.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PagedResponse;
import com.collegeerp.Backend.teacher.dto.TeacherRequest;
import com.collegeerp.Backend.teacher.dto.TeacherResponse;
import com.collegeerp.Backend.teacher.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherResponse> createTeacher(@Valid @RequestBody TeacherRequest request) {
        return ApiResponse.success("Teacher created", teacherService.createTeacher(request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<TeacherResponse>> getAllTeachers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(PagedResponse.from(teacherService.getAllTeachers(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherResponse> getTeacher(@PathVariable Long id) {
        return ApiResponse.success(teacherService.getTeacher(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request) {
        return ApiResponse.success("Teacher updated", teacherService.updateTeacher(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }
}
