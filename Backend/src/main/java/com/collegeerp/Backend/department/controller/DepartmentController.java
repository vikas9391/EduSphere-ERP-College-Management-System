package com.collegeerp.Backend.department.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PagedResponse;
import com.collegeerp.Backend.department.dto.DepartmentRequest;
import com.collegeerp.Backend.department.dto.DepartmentResponse;
import com.collegeerp.Backend.department.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created", departmentService.createDepartment(request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<DepartmentResponse>> getAllDepartments(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(PagedResponse.from(departmentService.getAllDepartments(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getDepartment(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department updated", departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
