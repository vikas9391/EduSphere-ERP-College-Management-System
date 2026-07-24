package com.collegeerp.Backend.tenant.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.tenant.dto.TenantDetailsResponse;
import com.collegeerp.Backend.tenant.dto.TenantRegistrationRequest;
import com.collegeerp.Backend.tenant.dto.TenantRegistrationResponse;
import com.collegeerp.Backend.tenant.dto.TenantStatusUpdateRequest;
import com.collegeerp.Backend.tenant.dto.TenantSubscriptionUpdateRequest;
import com.collegeerp.Backend.tenant.dto.TenantSummaryResponse;
import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;
import com.collegeerp.Backend.tenant.service.TenantProvisioningService;
import com.collegeerp.Backend.tenant.service.TenantStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Every endpoint here is restricted to SUPER_ADMIN by {@code SecurityConfig}
 * ({@code /api/tenants/**} requires {@code hasRole("SUPER_ADMIN")}) — registering,
 * suspending, subscription-managing, and deleting a college are all platform-operator
 * actions, not something any anonymous or tenant-scoped caller should be able to do.
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private static final Logger log = LoggerFactory.getLogger(TenantController.class);

    private final TenantProvisioningService provisioningService;
    private final TenantStatsService statsService;
    private final TenantRepository tenantRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantRegistrationResponse> register(@Valid @RequestBody TenantRegistrationRequest request) {
        log.info("Tenant registration request received for subdomain '{}'", request.getSubdomain());
        TenantRegistrationResponse response = provisioningService.register(request);
        return ApiResponse.success("College registered successfully", response);
    }

    @GetMapping
    public ApiResponse<List<TenantSummaryResponse>> list() {
        List<TenantSummaryResponse> tenants = tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Tenant::getCreatedAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .map(TenantSummaryResponse::from)
                .toList();
        return ApiResponse.success(tenants);
    }

    /**
     * A single college's info plus a live usage snapshot (staff/teacher/student counts,
     * departments, courses, subjects, enrollments) — read straight from that college's
     * own schema. Works even for a suspended college; suspension only blocks login.
     */
    @GetMapping("/{id}/details")
    public ApiResponse<TenantDetailsResponse> details(@PathVariable UUID id) {
        return ApiResponse.success(statsService.getDetails(id));
    }

    /**
     * Suspends or reactivates a college. A suspended college can't log in at all — every
     * account under it (admin, teacher, student) is blocked by {@code AuthController#login}
     * until this is flipped back on. Reversible; no data is touched.
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<TenantSummaryResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody TenantStatusUpdateRequest request) {
        Tenant tenant = provisioningService.updateStatus(id, request.isActive());
        String message = request.isActive() ? "College reactivated" : "College suspended";
        return ApiResponse.success(message, TenantSummaryResponse.from(tenant));
    }

    /**
     * Updates a college's subscription plan/status/expiry. Bookkeeping only — an expired
     * or cancelled subscription does NOT by itself block logins; suspend the college via
     * {@link #updateStatus} as well if you want that.
     */
    @PatchMapping("/{id}/subscription")
    public ApiResponse<TenantSummaryResponse> updateSubscription(
            @PathVariable UUID id, @Valid @RequestBody TenantSubscriptionUpdateRequest request) {
        Tenant tenant = provisioningService.updateSubscription(id, request);
        return ApiResponse.success("Subscription updated", TenantSummaryResponse.from(tenant));
    }

    /**
     * Irreversibly deletes a college — drops its entire schema and every record in it.
     * There is no confirmation step on the backend; the frontend must get explicit
     * confirmation from the super admin before ever calling this.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        provisioningService.deleteTenant(id);
        return ApiResponse.success("College permanently deleted", null);
    }
}
