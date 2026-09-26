package com.collegeerp.Backend.fee.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AssignFeeRequest(
    @NotNull Long studentId,
    @NotNull Long feeStructureId,
    @NotNull @DecimalMin("0.00") BigDecimal discount
) {}
