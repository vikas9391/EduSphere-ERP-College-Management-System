package com.collegeerp.Backend.fee.dto;
import java.math.BigDecimal;
import java.time.*;
public record FeeResponse(
    Long id, Long studentId, String studentName, Long feeStructureId, String feeName,
    String academicYear, Integer semester, BigDecimal discount, BigDecimal totalAmount,
    BigDecimal amountPaid, BigDecimal balance, String status, LocalDate dueDate
) {}
