package com.collegeerp.Backend.fee.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_reference", length = 120)
    private String transactionReference;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 60)
    private String receiptNumber;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(length = 500)
    private String notes;

    public enum PaymentMethod { CASH, BANK_TRANSFER, UPI, CARD, OTHER }
}
