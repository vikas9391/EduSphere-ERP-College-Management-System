package com.collegeerp.Backend.fee.entity;

import com.collegeerp.Backend.course.entity.Course;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_structures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeStructure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    private Integer semester;

    @Column(nullable = false)
    private String name;

    @Column(name = "tuition_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    @Column(name = "examination_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal examinationFee = BigDecimal.ZERO;

    @Column(name = "library_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal libraryFee = BigDecimal.ZERO;

    @Column(name = "other_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherFee = BigDecimal.ZERO;

    private LocalDate dueDate;
    private LocalDateTime createdAt;
}
