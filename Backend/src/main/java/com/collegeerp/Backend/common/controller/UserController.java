package com.collegeerp.Backend.common.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.common.dto.PagedResponse;
import com.collegeerp.Backend.common.dto.PasswordChangeRequest;
import com.collegeerp.Backend.common.dto.UserCreateRequest;
import com.collegeerp.Backend.common.dto.UserResponse;
import com.collegeerp.Backend.common.service.UserService;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Map<String, Object> profile = Map.of(
                "id", principal.getId(),
                "email", principal.getEmail(),
                "role", principal.getRole(),
                "permissions", principal.getPermissions(),
                "tenant", TenantContext.getCurrentTenant()
        );

        return ApiResponse.success(profile);
    }

    /**
     * Generalized user creation - replaces the old teacher-only account creation flow.
     * Any role can be assigned (HOD, Supervisor, Accountant, Librarian, or a custom
     * role), subject to {@code UserService}'s privilege-escalation guard.
     */
    @PreAuthorize("hasAuthority('CREATE_USER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ApiResponse.success("User created", userService.createUser(request, principal.getId()));
    }

    @PreAuthorize("hasAuthority('VIEW_USER')")
    @GetMapping
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(PagedResponse.from(userService.getAllUsers(pageable)));
    }

    @PreAuthorize("hasAuthority('VIEW_USER')")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    /**
     * Self-service password change - works for any authenticated staff/admin role,
     * including someone still flagged {@code mustChangePassword}. No permission gate
     * beyond being logged in: everyone is always allowed to change their own password.
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.changePassword(principal.getId(), request);
        return ApiResponse.success("Password updated", null);
    }
}
