package com.collegeerp.Backend.attendance.entity;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",
       uniqueConstraints = {
               @UniqueConstraint(columnNames = {
                       "enrollment_id",
                       "attendance_date"
               })
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_enrollment_id")
    private com.collegeerp.Backend.schoolclass.entity.ClassEnrollment classEnrollment;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private String status;

    private String remarks;

    private LocalDateTime createdAt;
}