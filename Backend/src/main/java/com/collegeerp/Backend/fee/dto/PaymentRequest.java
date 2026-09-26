package com.collegeerp.Backend.fee.dto;
import com.collegeerp.Backend.fee.entity.FeePayment.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PaymentRequest(
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull PaymentMethod paymentMethod,
    @Size(max=120) String transactionReference,
    @Size(max=500) String notes
) {}
