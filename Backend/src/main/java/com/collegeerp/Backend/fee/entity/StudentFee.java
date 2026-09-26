package com.collegeerp.Backend.fee.entity;

import com.collegeerp.Backend.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_fees", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "fee_structure_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentFee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public enum Status { PENDING, PARTIALLY_PAID, PAID, OVERDUE }
}
