package com.collegeerp.Backend.fee.controller;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.fee.dto.*;
import com.collegeerp.Backend.fee.entity.FeeStructure;
import com.collegeerp.Backend.fee.service.FeeService;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import com.collegeerp.Backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fees")
public class FeeController {
    private final FeeService feeService;
    private final StudentIdentityService studentIdentityService;

    public FeeController(FeeService feeService, StudentIdentityService studentIdentityService) {
        this.feeService = feeService;
        this.studentIdentityService = studentIdentityService;
    }

    @PreAuthorize("hasAuthority('MANAGE_FEES')")
    @PostMapping("/structures")
    public ApiResponse<FeeStructure> createStructure(@Valid @RequestBody FeeStructureRequest request) {
        return ApiResponse.success("Fee structure created", feeService.createStructure(request));
    }

    @PreAuthorize("hasAuthority('VIEW_FEES')")
    @GetMapping("/structures")
    public ApiResponse<List<FeeStructure>> structures() {
        return ApiResponse.success(feeService.getStructures());
    }

    @PreAuthorize("hasAuthority('MANAGE_FEES')")
    @PostMapping("/assign")
    public ApiResponse<FeeResponse> assign(@Valid @RequestBody AssignFeeRequest request) {
        return ApiResponse.success("Fee assigned", feeService.assign(request));
    }

    @PreAuthorize("hasAuthority('VIEW_FEES')")
    @GetMapping("/student/{studentId}")
    public ApiResponse<List<FeeResponse>> studentFees(@PathVariable Long studentId) {
        return ApiResponse.success(feeService.getStudentFees(studentId));
    }

    @PreAuthorize("hasAuthority('MANAGE_FEES')")
    @PostMapping("/{studentFeeId}/payments")
    public ApiResponse<PaymentResponse> recordPayment(@PathVariable Long studentFeeId,
                                                       @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success("Payment recorded", feeService.recordPayment(studentFeeId, request));
    }

    @PreAuthorize("hasAuthority('VIEW_FEES')")
    @GetMapping("/{studentFeeId}/payments")
    public ApiResponse<List<PaymentResponse>> payments(@PathVariable Long studentFeeId) {
        return ApiResponse.success(feeService.getPayments(studentFeeId));
    }

    @PreAuthorize("hasAuthority('VIEW_FEES')")
    @GetMapping("/{id}")
    public ApiResponse<FeeResponse> fee(@PathVariable Long id) {
        return ApiResponse.success(feeService.getFee(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<FeeResponse>> mine(Authentication authentication) {
        Long studentId = studentIdentityService.requireStudentId((UserPrincipal) authentication.getPrincipal());
        return ApiResponse.success(feeService.getStudentFees(studentId));
    }
}
