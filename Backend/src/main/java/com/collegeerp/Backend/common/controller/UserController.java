package com.collegeerp.Backend.common.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Map<String, Object> profile = Map.of(
                "id", principal.getId(),
                "email", principal.getEmail(),
                "role", principal.getRole(),
                "tenant", TenantContext.getCurrentTenant()
        );

        return ApiResponse.success(profile);
    }
}
