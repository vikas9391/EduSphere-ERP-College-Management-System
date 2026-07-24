package com.collegeerp.Backend.student.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PagedResponse;
import com.collegeerp.Backend.student.dto.StudentRequest;
import com.collegeerp.Backend.student.dto.StudentResponse;
import com.collegeerp.Backend.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.success("Student created", studentService.createStudent(request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<StudentResponse>> getAllStudents(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(PagedResponse.from(studentService.getAllStudents(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getStudent(@PathVariable Long id) {
        return ApiResponse.success(studentService.getStudent(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ApiResponse.success("Student updated", studentService.updateStudent(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }
}
