package com.collegeerp.Backend.fee.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record FeeStructureRequest(
    Long courseId,
    @NotBlank String academicYear,
    @Min(1) @Max(12) Integer semester,
    @NotBlank String name,
    @NotNull @DecimalMin("0.00") BigDecimal tuitionFee,
    @NotNull @DecimalMin("0.00") BigDecimal examinationFee,
    @NotNull @DecimalMin("0.00") BigDecimal libraryFee,
    @NotNull @DecimalMin("0.00") BigDecimal otherFee,
    LocalDate dueDate
) {}
