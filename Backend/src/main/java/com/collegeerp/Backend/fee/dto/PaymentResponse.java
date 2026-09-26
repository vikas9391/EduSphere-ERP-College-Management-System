package com.collegeerp.Backend.fee.dto;
import com.collegeerp.Backend.fee.entity.FeePayment.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentResponse(Long id, Long studentFeeId, BigDecimal amount, PaymentMethod paymentMethod,
                              String transactionReference, String receiptNumber, LocalDateTime paidAt, String notes) {}
