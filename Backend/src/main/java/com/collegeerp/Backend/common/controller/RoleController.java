package com.collegeerp.Backend.common.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PermissionInfo;
import com.collegeerp.Backend.common.dto.RoleRequest;
import com.collegeerp.Backend.common.dto.RoleResponse;
import com.collegeerp.Backend.common.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAuthority('CREATE_ROLE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success("Role created", roleService.createRole(request));
    }

    /** Any of the four role permissions is enough to list roles - used to populate
     *  the "assign role" dropdown as much as the role-management screen itself. */
    @PreAuthorize("hasAnyAuthority('CREATE_ROLE','EDIT_ROLE','DELETE_ROLE','ASSIGN_ROLE')")
    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles());
    }

    @PreAuthorize("hasAnyAuthority('CREATE_ROLE','EDIT_ROLE','DELETE_ROLE','ASSIGN_ROLE')")
    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.success("Role updated", roleService.updateRole(id, request));
    }

    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    /** Powers the role-builder permission checklist - every permission that exists, grouped by category. */
    @PreAuthorize("hasAnyAuthority('CREATE_ROLE','EDIT_ROLE')")
    @GetMapping("/permissions")
    public ApiResponse<List<PermissionInfo>> getAllPermissions() {
        return ApiResponse.success(roleService.getAllPermissions());
    }
}
